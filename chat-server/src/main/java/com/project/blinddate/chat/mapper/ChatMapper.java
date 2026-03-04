package com.project.blinddate.chat.mapper;

import com.project.blinddate.chat.domain.ChatMessage;
import com.project.blinddate.chat.domain.ChatRoom;
import com.project.blinddate.chat.dto.ChatMessageResponse;
import com.project.blinddate.chat.dto.ChatRoomResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChatMapper {

    ChatRoomResponse toRoomResponse(ChatRoom chatRoom);

    ChatMessageResponse toMessageResponse(ChatMessage message);
}


