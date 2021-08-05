package com.ftseoul.visitor.service;

import com.ftseoul.visitor.data.*;
import com.ftseoul.visitor.dto.*;
import com.ftseoul.visitor.encrypt.Seed;
import com.ftseoul.visitor.exception.PhoneDuplicatedException;
import com.ftseoul.visitor.exception.ResourceNotFoundException;
import com.ftseoul.visitor.service.sns.SMSService;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional
@Slf4j
public class ReserveService {

    private final ReserveRepository reserveRepository;
    private final VisitorRepository visitorRepository;
    private final StaffService staffService;
    private final StaffRepository staffRepository;
    private final VisitorService visitorService;
    private final SMSService smsService;
    private final QRcodeService qrCodeService;
    private final Seed seed;

    public ReserveListResponseDto findById(Long id) {
        Reserve reserve = reserveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserve", "id", id));
        List<VisitorDecryptDto> visitor = visitorRepository.findAllByReserveId(id)
                .stream().map(visitor1 -> VisitorDecryptDto.builder()
                .reserveId(visitor1.getReserveId())
                .phone(visitor1.getPhone())
                .name(visitor1.getName())
                .organization(visitor1.getOrganization())
                .build().decryptDto(seed)).collect(Collectors.toList());
        return ReserveListResponseDto.builder()
                .staff(staffService.decrypt(staffRepository.findById(reserve.getTargetStaff())
                        .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", reserve.getTargetStaff()))))
                .place(reserve.getPlace())
                .date(reserve.getDate())
                .id(reserve.getId())
                .purpose(reserve.getPurpose())
                .visitor(visitor)
                .build();
    }

    public List<ReserveResponseDto> findAllByNameAndPhone(SearchReserveRequestDto reserveRequestDto) {
        checkExistVisitorName(seed.encrypt(reserveRequestDto.getName()), seed.encrypt(reserveRequestDto.getPhone()));
        List<Visitor> visitorList = visitorRepository.findAllByNameAndPhone(seed.encrypt(reserveRequestDto.getName()),
                seed.encrypt(reserveRequestDto.getPhone()));
        List<ReserveResponseDto> reserveList = new ArrayList<>();
        for (int i = 0; i < visitorList.size(); i++) {
            int finalI = i;
            Reserve reserve = reserveRepository.findById(visitorList.get(i).getReserveId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reserve", "id", visitorList.get(finalI).getReserveId()));
            reserveList.add(ReserveResponseDto.builder()
                    .staff(staffService.decrypt(staffRepository.findById(reserve.getTargetStaff())
                            .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", reserve.getTargetStaff()))))
                    .visitor(visitorService.decryptDto(visitorList.get(i)))
                    .date(reserve.getDate())
                    .id(reserve.getId())
                    .purpose(reserve.getPurpose())
                    .place(reserve.getPlace())
                    .build());

        }
        return reserveList;
    }

    public List<ReserveListResponseDto> findReserveByVisitor(SearchReserveRequestDto requestDto) {
        checkExistVisitorName(seed.encrypt(requestDto.getName()), seed.encrypt(requestDto.getPhone()));
        List<Visitor> visitorList = visitorRepository.findAllByNameAndPhone(seed.encrypt(requestDto.getName()), seed.encrypt(requestDto.getPhone()));
        List<ReserveListResponseDto> responseDtos = new ArrayList<>();
        for (int i = 0; i < visitorList.size(); i++) {
            int finalI = i;
            Reserve reserve = reserveRepository.findById(visitorList.get(i).getReserveId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reserve", "id", visitorList.get(finalI).getReserveId()));
            List<VisitorDecryptDto> visitors = visitorRepository.findAllByReserveId(reserve.getId())
                    .stream().map(v -> VisitorDecryptDto.builder()
                    .name(v.getName())
                    .phone(v.getPhone())
                    .organization(v.getOrganization())
                    .build().decryptDto(seed)).collect(Collectors.toList());
            responseDtos
                    .add(ReserveListResponseDto.builder()
                            .id(reserve.getId())
                            .date(reserve.getDate())
                            .place(reserve.getPlace())
                            .purpose(reserve.getPurpose())
                            .staff(staffService.decrypt(staffRepository.findById(reserve.getTargetStaff())
                                    .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", reserve.getTargetStaff()))))
                            .visitor(visitors)
                            .build());
        }
        return responseDtos;
    }

    private void checkExistVisitorName(String name, String phone) {
        if (visitorRepository.findAllByName(name).size() == 0)
        {
            log.error("visitor name is not found");
            throw new ResourceNotFoundException("Visitor", "name", name);
        }
        if (visitorRepository.findAllByPhone(phone).size() == 0)
        {
            log.error("visitor phone is not phone");
            throw new ResourceNotFoundException("Visitor", "phone", phone);
        }
    }

    public boolean reserveDelete(Long reserve_id, ReserveDeleteRequestDto requestDto) {
        if (requestDto == null) {
            log.info("dto is null, reserve_id: " + reserve_id.toString());
            if (visitorRepository.findAllByReserveId(reserve_id).size() > 0) {
                reserveRepository.deleteById(reserve_id);
                visitorRepository.deleteAllByReserveId(reserve_id);
            }
            return true;
        }
        checkExistVisitorName(seed.encrypt(requestDto.getName()), seed.encrypt(requestDto.getPhone()));
        List<Visitor> list = visitorRepository.findAllByReserveId(reserve_id);
        if (list.size() == 0) {
            log.error("reserve id is not found: " + reserve_id.toString());
            throw new ResourceNotFoundException("Reserve", "id", reserve_id);
        }
        else {
            Visitor v = visitorRepository.findByNameAndPhoneAndReserveId(seed.encrypt(requestDto.getName()),
                    seed.encrypt(requestDto.getPhone()), reserve_id)
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Visitor", "name", requestDto.getName())
                    );
            visitorRepository.delete(v);
            log.info("Visitor delete: " + v);
        }
        if (list.size() == 1) {
            log.info("Reserve delete: " + reserve_id);
            reserveRepository.delete(reserveRepository.findById(reserve_id).get());
        }
        return true;
    }

    public Reserve saveReserve(ReserveVisitorDto reserveVisitorDto){
        checkDuplicatedPhone(reserveVisitorDto.getVisitor());
        Reserve reserve = Reserve.builder()
                .targetStaff(staffRepository.findByName(seed.encrypt(reserveVisitorDto.getTargetStaffName()))
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Staff", "name", reserveVisitorDto.getTargetStaffName())
                        ).getId())
                .place(reserveVisitorDto.getPlace())
                .purpose(reserveVisitorDto.getPurpose())
                .date(reserveVisitorDto.getDate())
                .build();
        log.info("save repository: " + reserve);
        return reserveRepository.save(reserve);
    }

    public boolean updateReserve(ReserveModifyDto reserveModifyDto) {
        Reserve reserve = reserveRepository
            .findById(reserveModifyDto.getReserveId())
            .orElseThrow(() -> new ResourceNotFoundException("Reserve", "id", reserveModifyDto.getReserveId()));
        Staff staff = staffRepository.findByName(seed.encrypt(reserveModifyDto.getTargetStaffName()))
            .orElseThrow(() -> new ResourceNotFoundException("Staff", "name", reserveModifyDto.getTargetStaffName()));
        reserve.update(reserveModifyDto.getPlace(), staff.getId(),
            reserveModifyDto.getPurpose(), reserveModifyDto.getDate());
        log.info("reserve update: " + reserve);
        reserveRepository.save(reserve);
        reserveModifyDto.encrypt(seed);
        List<Visitor> visitors = visitorService.updateVisitors(reserveModifyDto);
        smsService.sendMessage(new StaffDto(reserve.getId(), staff.getPhone(),
            reserveModifyDto.getPurpose(), reserveModifyDto.getPlace(),
            reserveModifyDto.getDate(), visitors));
        return true;
    }
    public void checkDuplicatedPhone(List<VisitorDto> visitorDto) {
        log.info("Check phone Duplication");
        boolean result = false;
        Set<String> phones = new HashSet<>();
        List<VisitorDto> collected = visitorDto
            .stream()
            .filter(visitor -> !phones.add(visitor.getPhone()))
            .collect(Collectors.toList());
        if (collected.size() > 0) {
            throw new PhoneDuplicatedException("전화번호 중복");
        }
    }
}
