package edu.stanford.slac.ad.eed.base_mongodb_lib.model;


import edu.stanford.slac.ad.eed.baselib.model.CaptureChanges;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.List;


@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@CaptureChanges
public class TestChangeModelList {
    @Id
    private String id;
    @CaptureChanges
    private List stringField1;
    @CaptureChanges
    private List boolField1;
    @CaptureChanges
    private List intField1;
    @CaptureChanges
    private List doubleField1;
    @CaptureChanges
    private List longField1;
    @CaptureChanges
    private List floatField1;
    @CaptureChanges
    private List dateField1;
    @CaptureChanges
    private List dateTimeField1;
}
