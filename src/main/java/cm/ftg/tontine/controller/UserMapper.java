package cm.ftg.tontine.controller;

import cm.ftg.tontine.service.UserResult;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(UserResult result) {
        return new UserResponse(
            result.userId(),
            result.email(),
            result.phone(),
            result.firstName(),
            result.lastName(),
            result.dateOfBirth(),
            result.role() != null ? result.role().name() : null,
            result.region() != null ? result.region().name() : null,
            result.cniNumber(),
            result.createdAt()
        );
    }
}
