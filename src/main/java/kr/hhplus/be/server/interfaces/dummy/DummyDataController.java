package kr.hhplus.be.server.interfaces.dummy;

import kr.hhplus.be.server.domain.dummy.DummyDataInsertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class DummyDataController {

    private final DummyDataInsertService dummyDataInsertService;

    @PostMapping("/insert")
    public ResponseEntity<String> insertData(@RequestParam(defaultValue = "100000") int count) {
        dummyDataInsertService.bulkInsertOrderItems(count);
        return ResponseEntity.ok("삽입 완료: " + count);
    }

    @PostMapping("/insertProduct")
    public ResponseEntity<String> insertProduct(@RequestParam(defaultValue = "100000") int count) {
        dummyDataInsertService.bulkInsertProducts(count);
        return ResponseEntity.ok("삽입 완료 : " + count);
    }

    @PostMapping("/insertUser")
    public ResponseEntity<String> insertUser(@RequestParam(defaultValue = "1000") int count) {
        dummyDataInsertService.bulkInsertUsers(count);
        return ResponseEntity.ok("삽입 완료 : " + count);
    }
}
