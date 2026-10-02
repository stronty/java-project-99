package hexlet.code.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateDTO {
    @Email
    private String email;
    private String firstName;
    private String lastName;
    @Size(min = 3)
    private String password;
}