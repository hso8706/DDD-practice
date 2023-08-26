package com.myshop.order.command.domain;

//주문 출고 상태
public enum OrderState {
    //결제 대기, 출고 준비중, 출고됨, 배송중, 배송 완료, 취소됨
    PAYMENT_WAITING, PREPARING, SHIPPED, DELIVERING, DELIVERY_COMPLETED, CANCELED
}
