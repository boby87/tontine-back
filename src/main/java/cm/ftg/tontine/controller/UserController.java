package cm.ftg.tontine.controller;

import cm.ftg.tontine.controller.doc.UserControllerDoc;
import cm.ftg.tontine.service.CreateUserRequest;
import cm.ftg.tontine.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController implements UserControllerDoc {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @Override
    @PostMapping("/register")
    public ResponseEntity<UserResponse> inscrire(@Valid @RequestBody CreateUserRequest request) {
        log.debug("POST /api/v1/users/register — VirtualThread={}, thread={}",
            Thread.currentThread().isVirtual(), Thread.currentThread().getName());

        var result = userService.inscrireUtilisateur(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(userMapper.toResponse(result));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> consulter(@PathVariable String id) {
        log.debug("GET /api/v1/users/{} — VirtualThread={}, thread={}",
            id, Thread.currentThread().isVirtual(), Thread.currentThread().getName());

        var result = userService.findById(id);
        return ResponseEntity.ok(userMapper.toResponse(result));
    }
}
