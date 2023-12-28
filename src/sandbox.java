import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthenticationController {

    @PostMapping("/authenticateUser")
    public String authenticateUser(@RequestBody String username, @RequestBody String password) {
        return Login.authenticateUser(username, password);
    }
}
