package com.github.dinhnn.umd;

import java.io.IOException;
import java.io.OutputStream;

import io.vertx.core.buffer.Buffer;

public class BufferOutputStream extends OutputStream {
	private final Buffer buff;

	public BufferOutputStream() {
		this.buff = Buffer.buffer();
	}
	public BufferOutputStream(Buffer buff) {
		this.buff = buff;
	}

	@Override
	public void write(int b) throws IOException {
		buff.appendByte((byte) b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		buff.appendBytes(b, off, len);
	}

	@Override
	public void close() throws IOException {
	}
	public Buffer getBuff() {
		return buff;
	}
}
