package kr.hhplus.be.server.domain.dummy;

import jakarta.persistence.EntityManagerFactory;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.instancio.Instancio;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

import static org.instancio.Select.field;


@Service
@RequiredArgsConstructor
public class DummyDataInsertService {

    private final EntityManagerFactory emf;

    public void bulkInsertOrderItems(int count) {
        SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
        StatelessSession session = sessionFactory.openStatelessSession();
        Transaction tx = session.beginTransaction();

        for (int i = 0; i < count; i++) {
            OrderItem item = Instancio.of(OrderItem.class)
                    .supply(field("productId"), () -> ThreadLocalRandom.current().nextLong(1L, 1001L)) // 1~101 포함
                    .supply(field("quantity"), () -> ThreadLocalRandom.current().nextInt(1, 10))
                    .supply(field("createdAt"), () -> {
                        LocalDateTime base = LocalDateTime.now().minusDays(1);
                        return base.plusMinutes(ThreadLocalRandom.current().nextInt(0, 4320));
                    })
                    .create();

            session.insert(item);

            if (i % 1000 == 0) {
                System.out.println("Inserted: " + i);
            }
        }

        tx.commit();
        session.close();
    }

    public void bulkInsertProducts(int count) {
        SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
        StatelessSession session = sessionFactory.openStatelessSession();
        Transaction tx = session.beginTransaction();

        for (int i = 0; i < count; i++) {
            int finalI = i;
            Product product = Instancio.of(Product.class)
                    .supply(field("name"), () -> "상품-" + finalI)
                    .supply(field("price"), () -> ThreadLocalRandom.current().nextInt(1000, 100000))
                    .supply(field("stock"), () -> ThreadLocalRandom.current().nextInt(1, 100))
                    .supply(field("createdAt"), () -> {
                        LocalDateTime now = LocalDateTime.now();
                        return now.minusDays(ThreadLocalRandom.current().nextInt(0, 365));
                    })
                    .create();

            session.insert(product);

            if (i % 1000 == 0) {
                System.out.println("Inserted Product: " + i);
            }
        }

        tx.commit();
        session.close();
    }

    public void bulkInsertUsers(int count) {
        SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
        StatelessSession session = sessionFactory.openStatelessSession();
        Transaction tx = session.beginTransaction();

        for (int i = 0; i < count; i++) {
            int finalI = i;
            User user = Instancio.of(User.class)
                    .supply(field("name"), () -> "user-" + finalI)
                    .supply(field("point"), () -> ThreadLocalRandom.current().nextInt(0, 100_000))
                    .create();

            session.insert(user);

            if (i % 1000 == 0) {
                System.out.println("Inserted User: " + i);
            }
        }

        tx.commit();
        session.close();
    }


}