package org.logging.to;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.logging.entity.AlertProfile;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AlertProfileTO {
    private String profileName;
    private String criteria;
    private String notifyEmail;
    public AlertProfile toEntity()
    {
        AlertProfile alertProfile = new AlertProfile();
        alertProfile.setProfileName(this.profileName);
        alertProfile.setCriteria(this.criteria);
        alertProfile.setNotifyEmail(this.notifyEmail);
        return alertProfile;
    }
}
