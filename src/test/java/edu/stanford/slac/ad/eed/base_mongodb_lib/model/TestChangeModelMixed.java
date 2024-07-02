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
public class TestChangeModelMixed {
    @Id
    private String id;
    @CaptureChanges
    private String stringField1;
    @CaptureChanges
    private List boolField1;
    @CaptureChanges
    private TestChangeModelPrimitive classPrimitiveField;
    @CaptureChanges
    private List<TestChangeModelPrimitive> classPrimitiveListField;
    @CaptureChanges
    private List<TestChangeModelArray> classListArrayField;
    @CaptureChanges
    private List<List<TestChangeModelArray>> classListListArrayField;
}
