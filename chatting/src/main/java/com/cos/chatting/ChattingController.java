package com.cos.chatting;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitResult;

@CrossOrigin
@RestController
public class ChattingController {
	
	Sinks.Many<String> sink; // Process 지속적 응답
	
	// multicast() 새로 들어온 데이터만 응답 받음 hot (시퀀스 = 스트림)
	// replay() 기존 데이터 + 새로운 데이터 cold 시퀀스

	public ChattingController() {
		this.sink = Sinks.many().multicast().onBackpressureBuffer();
	}

	@PostMapping("/send")
	public void send(@RequestBody Chatting chatting) {
		String message = chatting.getUsername()+" : "+ chatting.getMessage();
		EmitResult res = sink.tryEmitNext(message);
	}

	// data : 실제값\n\n
	@GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> sse() { // ServerSentEvent의 ContentType은 text event stream
		return sink.asFlux().map(e -> ServerSentEvent.builder(e).build()).doOnCancel(() -> {
			sink.asFlux().blockLast(); // SSE 끊기 (끊어줘야 다시 SSE 연결요청을 해도 잘 동작함)

		}); // 구독
	}

}
