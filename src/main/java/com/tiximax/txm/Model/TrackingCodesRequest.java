package com.tiximax.txm.Model;

import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class TrackingCodesRequest {
    private List<String> selectedTrackingCodes;

    public List<String> getSelectedTrackingCodes() {
        return selectedTrackingCodes;
    }
    public void setSelectedTrackingCodes(List<String> selectedTrackingCodes) {
        this.selectedTrackingCodes = selectedTrackingCodes;
    }
}

