package com.ftseoul.visitor.service;

import com.ftseoul.visitor.data.ReserveRepository;
import com.ftseoul.visitor.data.Visitor;
import com.ftseoul.visitor.data.VisitorRepository;
import com.ftseoul.visitor.dto.DateFoundResponseDto;
import com.ftseoul.visitor.dto.UpdateVisitorStatusDto;
import com.ftseoul.visitor.dto.VisitorDecryptWithIdDto;
import com.ftseoul.visitor.dto.payload.Response;
import com.ftseoul.visitor.dto.payload.VisitorStatusInfo;
import com.ftseoul.visitor.encrypt.Seed;
import com.ftseoul.visitor.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class InfoService {
    private final ReserveRepository reserveRepository;
    private final VisitorRepository visitorRepository;
    private final Seed seed;

    public List<DateFoundResponseDto> findAllByDate(LocalDate date) {
        LocalDateTime startDay = date.atStartOfDay();
        LocalDateTime endDay = date.plusDays(1).atStartOfDay();

        List<DateFoundResponseDto> result = reserveRepository.findAllReserveWithStaffByDate(startDay, endDay);
        if (result == null) {
            throw new ResourceNotFoundException("Reserve", "Date", date.toString());
        }

        for (DateFoundResponseDto response : result) {
            response.setStaffName(seed.decrypt(response.getStaffName()));
            response.setStaffPhone(seed.decrypt(response.getStaffPhone()));
            List<Visitor> visitorList = visitorRepository.findAllByReserveId(response.getId());
            response.setVisitors(decryptVisitorList(visitorList));
        }
        return result;
    }

    private List<VisitorDecryptWithIdDto> decryptVisitorList(List<Visitor> visitorList) {
        return visitorList
            .stream()
            .map(v -> new VisitorDecryptWithIdDto(v.getId(), v.getReserveId(), v.getName(),
                v.getPhone(), v.getOrganization(), v.getStatus()).decryptDto(seed))
            .collect(Collectors.toList());
    }

    public VisitorStatusInfo changeVisitorStatus(UpdateVisitorStatusDto dto) {
        Visitor visitor = visitorRepository.findById(dto.getVisitor().getId())
            .orElseThrow(() -> new ResourceNotFoundException("Visitor", "VisitorId", dto.getVisitor().getId()));
        if (visitor.getStatus() == dto.getVisitor().getStatus()) {
            return null;
        }
        visitor.updateStatus(dto.getVisitor().getStatus());
        Visitor savedVisitor = visitorRepository.save(visitor);
        return new VisitorStatusInfo(savedVisitor.getId(), savedVisitor.getStatus());
    }

}