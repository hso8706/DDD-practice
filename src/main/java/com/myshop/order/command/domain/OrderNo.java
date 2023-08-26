package com.myshop.order.command.domain;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

//Order 엔티티의 식별자 도메인; 밸류 타입
@Embeddable
public class OrderNo implements Serializable {
    @Column(name = "order_number")
    private String number;

    protected OrderNo() {
    }

    public OrderNo(String number) {
        this.number = number;
    }

    public String getNumber() {
        return number;
    }

    //식별자를 기준으로 Order 엔티티를 구분하는 메서드
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderNo orderNo = (OrderNo) o;
        return Objects.equals(number, orderNo.number);
    }

    //number를 해싱하여 해시코드로 반환하는 메서드
    @Override
    public int hashCode() {
        return Objects.hash(number);
    }

    public static OrderNo of(String number) {
        return new OrderNo(number);
    }
}
