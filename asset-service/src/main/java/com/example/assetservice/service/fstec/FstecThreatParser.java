package com.example.assetservice.service.fstec;

import com.example.assetservice.model.entity.Threat;
import java.io.InputStream;
import java.util.List;

public interface FstecThreatParser {
    List<Threat> parse(InputStream inputStream) throws Exception;
}
