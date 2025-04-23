package kr.hhplus.be.server.interfaces.UserPoint;

import kr.hhplus.be.server.application.user.UserPointFacade;
import kr.hhplus.be.server.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/point")
public class UserPointController {

    private final UserPointFacade userPointFacade;

    @PostMapping("/{userId}/charge")
    public ResponseEntity<User> chargePoint(@RequestBody UserPointRequest request) {
        User updatedUser = userPointFacade.chargePoint(request.getUserId(), request.getChargeAmount());
        return ResponseEntity.ok(updatedUser);
    }

}
