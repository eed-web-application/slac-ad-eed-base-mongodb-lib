package edu.stanford.slac.ad.eed.base_mongodb_lib.model;


import edu.stanford.slac.ad.eed.baselib.model.CaptureChanges;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
public class TestChangeModel1 {
    @Id
    private String id;
    @CaptureChanges
    private String stringField1;
    @CaptureChanges
    private Boolean boolField1;
}
