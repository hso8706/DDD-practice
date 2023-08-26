package com.myshop.common.event;

import org.springframework.context.ApplicationEventPublisher;

//이벤트
public class Events {
    /*
    - ApplicationEventPublisher를 저장하는 static 변수
    - ApplicationEventPublisher : 스프링 프레임워크에서 이벤트를 발행하는 인터페이스
     */
    private static ApplicationEventPublisher publisher;

    static void setPublisher(ApplicationEventPublisher publisher) {
        Events.publisher = publisher;
    }

    //이벤트 발행 메서드
    public static void raise(Object event) {
        if (publisher != null) {
            publisher.publishEvent(event);
        }
    }
}
