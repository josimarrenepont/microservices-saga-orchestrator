package br.com.microservices.orchestrated.inventoryservice.core.service;

import br.com.microservices.orchestrated.inventoryservice.core.dto.Event;
import br.com.microservices.orchestrated.inventoryservice.core.dto.History;
import br.com.microservices.orchestrated.inventoryservice.core.dto.Order;
import br.com.microservices.orchestrated.inventoryservice.core.dto.OrderProducts;
import br.com.microservices.orchestrated.inventoryservice.core.model.Inventory;
import br.com.microservices.orchestrated.inventoryservice.core.model.OrderInventory;
import br.com.microservices.orchestrated.inventoryservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.inventoryservice.core.repository.InventoryRepository;
import br.com.microservices.orchestrated.inventoryservice.core.repository.OrderInventoryRepository;
import br.com.microservices.orchestrated.inventoryservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.security.oauthbearer.internals.secured.ValidateException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static br.com.microservices.orchestrated.inventoryservice.core.enums.ESagaStatus.*;

@Service
@AllArgsConstructor
@Slf4j
public class InventoryService {

    private static final String CURRENT_SOURCE = "INVENTORY_SERVICE";
    private final JsonUtil jsonUtil;
    private final KafkaProducer producer;
    private final InventoryRepository inventoryRepository;
    private final OrderInventoryRepository orderInventoryRepository;

    public void updateInventory(Event event){
        try{
            checkCurrentValidation(event);
            createOrderInventory(event);
            updateInventory(event.getPayload());
            handleSuccess(event);
        } catch (Exception ex){
            log.error("Error trying to update inventory: ", ex);
            handleFailCurrentNotExecuted(event, ex.getMessage());
        }
        producer.sendEvent(jsonUtil.toJson(event));
    }

    private void checkCurrentValidation(Event event){
        if(orderInventoryRepository.existsByOrderIdAndTransactionId(
                event.getPayload().getId(), event.getTransactionId())){
            throw new ValidateException("There's another trasanction for this validation.");
        }
    }
    private void createOrderInventory(Event event) {
        event
                .getPayload()
                .getProducts()
                .forEach(product -> {
                    var inventory = findInventoryByProductCode(product.getProduct().getCode());
                    var orderInventory = createOrderInventory(event, product, inventory);
                    orderInventoryRepository.save(orderInventory);
                });
    }
    private OrderInventory createOrderInventory(Event event, OrderProducts product, Inventory inventory){
        return OrderInventory
                .builder()
                .inventory(inventory)
                .oldQuantity(inventory.getAvailable())
                .orderQuantity(product.getQuantity())
                .newQuantity(inventory.getAvailable() - product.getQuantity())
                .orderId(event.getPayload().getId())
                .transactionId(event.getTransactionId())
                .build();
    }
    private void updateInventory(Order order){
        order
                .getProducts()
                .forEach(product -> {
                    var inventory = findInventoryByProductCode(product.getProduct().getCode());
                    chekInventory(inventory.getAvailable(), product.getQuantity());
                    inventory.setAvailable(inventory.getAvailable() - product.getQuantity());
                    inventoryRepository.save(inventory);
                });
    }
    private void chekInventory(int available, int orderQuantity){
        if(orderQuantity > available){
            throw new ValidateException("Product is out of stock!");
        }
    }
    private void handleSuccess(Event event){
        event.setStatus(SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Inventory Updated successfully");
    }
    private void addHistory(Event event, String message){
        var history = History
                .builder()
                .source(event.getSource())
                .status(event.getStatus())
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
        event.addToHistory(history);
    }
    private void handleFailCurrentNotExecuted(Event event, String message){
        event.setStatus(ROLLBACK_PENDING);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Fail to update inventory: ".concat(message));
    }
    public void rollbackInventory(Event event){
        event.setStatus(FAIL);
        event.setSource(CURRENT_SOURCE);
        try{
            returnInventoryToPreviousValues(event);
            addHistory(event, "Rollback executed for inventory!");
        } catch (Exception ex){
            addHistory(event, "Rollback not executed for inventory. ".concat(ex.getMessage()));
        }
        producer.sendEvent(jsonUtil.toJson(event));
    }
    private void returnInventoryToPreviousValues(Event event){
        orderInventoryRepository.findByOrderIdAndTransactionId(event.getPayload().getId(), event.getTransactionId())
                .forEach(orderInventory -> {
                    var inventory = orderInventory.getInventory();
                    inventory.setAvailable(orderInventory.getOldQuantity());
                    inventoryRepository.save(inventory);
                    log.info("Restored inventory for order {} from {} to {}",
                            event.getPayload().getId(), orderInventory.getNewQuantity(), inventory.getAvailable());
                });
    }
    private Inventory findInventoryByProductCode(String productCode){
        return inventoryRepository
                .findByProductCode(productCode)
                .orElseThrow(() -> new ValidateException("Inventory not found by informed product."));
    }
}
