import { Client, Message } from "@stomp/stompjs";
import {
  ChangeEvent,
  FormEvent,
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";
import { useSubscription } from "react-stomp-hooks";
import { UserEntity } from "../types";
import { sendLobbyAction } from "../utils/stompUtils";
import { isEvent, isUserEntity } from "../utils/typeGuards";

type LobbyChatMessage = {
  author: UserEntity;
  content: string;
};

type LobbyChatProps = {
  uri: string;
  stompClient: Client;
};

const LobbyChat = (props: LobbyChatProps) => {
  const [chatMessages, setChatMessages] = useState<LobbyChatMessage[]>([]);
  const [textMessage, setTextMessage] = useState("");
  const messageBoxRef = useRef<HTMLDivElement>(null);

  const handleTextMessageChange = (e: ChangeEvent<HTMLInputElement>) => {
    e.preventDefault();
    setTextMessage(e.target.value);
  };

  const handleSendMessage = (e: FormEvent) => {
    e.preventDefault();
    sendLobbyAction(props.stompClient, "NEW_TEXT_MESSAGE", props.uri, {
      messageContent: textMessage,
    });
    setTextMessage("");
  };

  const handleLobbyReply = useCallback((message: Message) => {
    const messageBody: unknown = JSON.parse(message.body);

    if (!isEvent(messageBody)) {
      return;
    }

    const { id, data } = messageBody;

    if (id === "NEW_TEXT_MESSAGE") {
      const { messageAuthor, messageContent } = data;

      if (!isUserEntity(messageAuthor)) {
        return;
      }

      if (!(typeof messageContent === "string")) {
        return;
      }

      const newMessage: LobbyChatMessage = {
        author: messageAuthor,
        content: messageContent,
      };

      setChatMessages((prev) => [...prev, newMessage]);
    }
  }, []);

  useEffect(() => {
    if (messageBoxRef.current) {
      const scroll =
        messageBoxRef.current.scrollHeight - messageBoxRef.current.clientHeight;
      messageBoxRef.current.scrollTo(0, scroll);
    }
  }, [chatMessages]);

  useSubscription("/user/queue/reply/lobby", handleLobbyReply);

  return (
    <>
      <div
        className="border-2 p-5 border-blue-500 bg-blue-200 h-96 max-h-96 overflow-y-scroll"
        ref={messageBoxRef}
      >
        {chatMessages.map((message, idx) => (
          <div key={idx}>
            <div className="text-blue-500">
              {message.author.username}:{" "}
              <span className="text-blue-600">{message.content}</span>
            </div>
          </div>
        ))}
      </div>

      <form className="w-full max-w-sm bottom-0" onSubmit={handleSendMessage}>
        <div className="flex border-b border-blue-500 py-2">
          <input
            className="appearance-none bg-transparent border-none w-full text-gray-700 mr-3 py-1 px-2 leading-tight focus:outline-none"
            type="text"
            onChange={handleTextMessageChange}
            value={textMessage}
            required
          />
          <button
            className="flex-shrink-0 bg-blue-500 hover:bg-blue-700 border-blue-500 hover:border-blue-700 text-sm border-4 text-white py-1 px-2"
            type="button"
          >
            Send Message
          </button>
        </div>
      </form>
    </>
  );
};

export default LobbyChat;
