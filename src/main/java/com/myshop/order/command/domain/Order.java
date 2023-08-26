package com.myshop.order.command.domain;

import com.myshop.common.event.Events;
import com.myshop.common.jpa.MoneyConverter;
import com.myshop.common.model.Money;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

//주문 도메인
@Entity
@Table(name = "purchase_order")
@Access(AccessType.FIELD)
public class Order {

    //Order 엔티티의 식별자
    @EmbeddedId
    private OrderNo number;

    @Version
    private long version;

    @Embedded
    private Orderer orderer;

    //주문 1개에 최소 한 개 이상의 상품을 주문해야 한다. == 최소 한 개 이상의 주문 항목(orderLine)이 필요
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "order_line", joinColumns = @JoinColumn(name = "order_number"))
    @OrderColumn(name = "line_idx")
    private List<OrderLine> orderLines;

    //총 주문 금액은 각 상품의 구매 가격 합의 총합
    @Convert(converter = MoneyConverter.class)
    @Column(name = "total_amounts")
    private Money totalAmounts;

    //주문 시 배송지 정보를 반드시 지정해야 한다
    @Embedded
    private ShippingInfo shippingInfo;

    //주문 상태를 나타내는 필드
    //주문 취소는 출고 전에만 가능하다.
    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private OrderState state;

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    protected Order() {
    }

    public Order(OrderNo number, Orderer orderer, List<OrderLine> orderLines,
                 ShippingInfo shippingInfo, OrderState state) {
        setNumber(number);
        setOrderer(orderer);
        setOrderLines(orderLines);//최소 하나 이상의 주문 항목
        setShippingInfo(shippingInfo);//배송지 정보 필수
        this.state = state;
        this.orderDate = LocalDateTime.now();
        Events.raise(new OrderPlacedEvent(number.getNumber(), orderer, orderLines, orderDate));
    }

    private void setNumber(OrderNo number) {
        if (number == null) throw new IllegalArgumentException("no number");
        this.number = number;
    }

    private void setOrderer(Orderer orderer) {
        if (orderer == null) throw new IllegalArgumentException("no orderer");
        this.orderer = orderer;
    }

    /*
    - orderLines 필드를 채우기 위한 setter
    - 검증 로직 추가
    - private을 통해 외부에서 값을 변경하지 못하도록 설정
    - 가격 총합을 구하는 로직 추가
     */
    private void setOrderLines(List<OrderLine> orderLines) {
        verifyAtLeastOneOrMoreOrderLines(orderLines); //최소 한 개 이상의 주문 항목이 있는지 검증
        this.orderLines = orderLines;
        calculateTotalAmounts();
    }

    //최소 한 개 이상의 주문 항목이 있는지 검증
    private void verifyAtLeastOneOrMoreOrderLines(List<OrderLine> orderLines) {
        if (orderLines == null || orderLines.isEmpty()) {
            throw new IllegalArgumentException("no OrderLine");
        }
    }

    //각 주문 항목(orderLines)에 해당하는 총 가격을 모두 더하여 totalAmounts를 설정
    private void calculateTotalAmounts() {
        this.totalAmounts = new Money(orderLines.stream()
                .mapToInt(x -> x.getAmounts().getValue()).sum());
    }

    //배송지 정보를 검증 및 추가하는 로직
    private void setShippingInfo(ShippingInfo shippingInfo) {
        if (shippingInfo == null) throw new IllegalArgumentException("no shipping info");
        this.shippingInfo = shippingInfo;
    }

    public OrderNo getNumber() {
        return number;
    }

    public long getVersion() {
        return version;
    }

    public Orderer getOrderer() {
        return orderer;
    }

    public Money getTotalAmounts() {
        return totalAmounts;
    }

    public ShippingInfo getShippingInfo() {
        return shippingInfo;
    }

    public OrderState getState() {
        return state;
    }

    /*
    - 배송지 변경 메서드
    - 출고 여부 파악
    - 새로운 배송지 정보로 수정
    - 배송지 수정 이벤트 발행
     */
    public void changeShippingInfo(ShippingInfo newShippingInfo) {
        verifyNotYetShipped();
        setShippingInfo(newShippingInfo);
        Events.raise(new ShippingInfoChangedEvent(number, newShippingInfo));
    }

    /*
    - 주문 취소 메서드
    - 출고 여부 파악
    - 주문 취소 상태로 변경
    - 주문 취소 이벤트 발행
     */
    public void cancel() {
        verifyNotYetShipped();
        this.state = OrderState.CANCELED;
        Events.raise(new OrderCanceledEvent(number.getNumber()));
    }

    //출고 여부 파악 메서드
    private void verifyNotYetShipped() {
        if (!isNotYetShipped())
            throw new AlreadyShippedException();
    }

    //출고 전(== 결제 대기 혹은 출고 준비 중)인지 확인하는 메서드
    public boolean isNotYetShipped() {
        return state == OrderState.PAYMENT_WAITING || state == OrderState.PREPARING;
    }

    public List<OrderLine> getOrderLines() {
        return orderLines;
    }

    public boolean matchVersion(long version) {
        return this.version == version;
    }

    public void startShipping() {
        verifyShippableState();
        this.state = OrderState.SHIPPED;
        Events.raise(new ShippingStartedEvent(number.getNumber()));
    }

    private void verifyShippableState() {
        verifyNotYetShipped();
        verifyNotCanceled();
    }

    private void verifyNotCanceled() {
        if (state == OrderState.CANCELED) {
            throw new OrderAlreadyCanceledException();
        }
    }
}
