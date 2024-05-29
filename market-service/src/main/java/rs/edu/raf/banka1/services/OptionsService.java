package rs.edu.raf.banka1.services;

import rs.edu.raf.banka1.model.OptionsModel;
import rs.edu.raf.banka1.model.dtos.OptionsDto;

import java.util.List;
import java.util.Optional;

public interface OptionsService {
    List<OptionsDto> getOptionsByTicker(String ticker);
    Optional<OptionsModel> findById(Long id);
    List<OptionsModel> getAllOptions();
}
