package org.logging.to;

import org.logging.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserTO {
    private String name;
    private String email;
    private String password;

    public UserTO(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public User toEntity()
    {
        User newUser = new User();
        newUser.setName(this.name);
        newUser.setEmail(this.email);
        newUser.setPassword(this.getPassword());
        return newUser;
    }
}
