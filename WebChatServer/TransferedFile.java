package prozWebChat;

import java.nio.ByteBuffer;

public class TransferedFile {
	private ByteBuffer buffer = null;
	private String name = null;

	public TransferedFile(ByteBuffer toBuffer, String toName) {
		this.buffer = toBuffer;
		this.name = toName;
	}

	public ByteBuffer getBuffer() {
		return buffer;
	}
	
	public String getName() {
		return name;
	}

}
