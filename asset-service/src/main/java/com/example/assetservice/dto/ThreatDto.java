package com.example.assetservice.dto;

import com.example.assetservice.model.entity.Threat;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class ThreatDto {
    private Long id;
    private String name;
    private String description;
    private String source;
    private String objectAffected;
    private boolean confidentiality;
    private boolean integrity;
    private boolean availability;
    private LocalDate inclusionDate;
    private LocalDate lastModified;
    private String status;
    private String notes;

    public static ThreatDto fromEntity(Threat threat) {
        ThreatDto dto = new ThreatDto();
        dto.setId(threat.getId());
        dto.setName(threat.getName());
        dto.setDescription(threat.getDescription());
        dto.setSource(threat.getSource());
        dto.setObjectAffected(threat.getObjectAffected());
        dto.setConfidentiality(threat.isConfidentiality());
        dto.setIntegrity(threat.isIntegrity());
        dto.setAvailability(threat.isAvailability());
        dto.setInclusionDate(threat.getInclusionDate());
        dto.setLastModified(threat.getLastModified());
        dto.setStatus(threat.getStatus());
        dto.setNotes(threat.getNotes());
        return dto;
    }
}