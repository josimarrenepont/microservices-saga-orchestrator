package br.com.microservices.orchestrated.orderservice.core.repository;

import br.com.microservices.orchestrated.orderservice.core.document.Event;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends MongoRepository<Event, String> {

    List<Event> findAllByOrderByCreateAtDesc();

    Optional<Event> findTop1ByOrderIdOderByCreatedAtDesc(String orderId);
    Optional<Event> findTop1ByTransactionIdOderByCreatedAtDesc(String transactionId);
}
