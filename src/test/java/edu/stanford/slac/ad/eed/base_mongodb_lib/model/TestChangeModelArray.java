package edu.stanford.slac.ad.eed.base_mongodb_lib.model;


import edu.stanford.slac.ad.eed.baselib.model.CaptureChanges;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@CaptureChanges
public class TestChangeModelArray {
    @Id
    private String id;
    @CaptureChanges
    private String[] stringField1;
    @CaptureChanges
    private Boolean[] boolField1;
    @CaptureChanges
    private Integer[] intField1;
    @CaptureChanges
    private Double[] doubleField1;
    @CaptureChanges
    private Long[] longField1;
    @CaptureChanges
    private Float[] floatField1;
    @CaptureChanges
    private LocalDate[] dateField1;
    @CaptureChanges
    private LocalDateTime[] dateTimeField1;
}
