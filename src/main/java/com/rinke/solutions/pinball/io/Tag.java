package com.rinke.solutions.pinball.io;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class Tag {
	
	public final TagType type;
	public int length;
	public byte[] payload;
	public CompressionType compressionType;
	private DataOutputStream dos;
	private ByteArrayOutputStream bos;

	public Tag(TagType type, byte[] payload, CompressionType ct) {
		super();
		this.type = type;
		this.length = payload.length;
		this.payload = payload;
		this.compressionType = ct;
	}

	public Tag(TagType type) {
		super();
		this.type = type;
		bos = new ByteArrayOutputStream();
		dos = new DataOutputStream(bos);
	}

	@Override
	public String toString() {
		return String.format("Tag [type=%s, length=%s, payload=%s, compressionType=%s]", type, length, Arrays.toString(payload), compressionType);
	}

	public static Tag create(TagType type) {
		Tag tag = new Tag(type);
		return tag;
	}

	public Tag write(int b) throws IOException {
		dos.write(b);
		return this;
	}

	public Tag write(byte[] b) throws IOException {
		dos.write(b);
		return this;
	}

	public Tag write(byte[] b, int off, int len) throws IOException {
		dos.write(b, off, len);
		return this;
	}

	public final Tag writeBoolean(boolean v) throws IOException {
		dos.writeBoolean(v);
		return this;
	}

	public final Tag writeByte(int v) throws IOException {
		dos.writeByte(v);
		return this;
	}

	public final Tag writeShort(int v) throws IOException {
		dos.writeShort(v);
		return this;
	}

	public final Tag writeChar(int v) throws IOException {
		dos.writeChar(v);
		return this;
	}

	public final Tag writeInt(int v) throws IOException {
		dos.writeInt(v);
		return this;
	}

	public final Tag writeLong(long v) throws IOException {
		dos.writeLong(v);
		return this;
	}

	public final Tag writeFloat(float v) throws IOException {
		dos.writeFloat(v);
		return this;
	}

	public final Tag writeDouble(double v) throws IOException {
		dos.writeDouble(v);
		return this;
	}

	public final Tag writeBytes(String s) throws IOException {
		dos.writeBytes(s);
		return this;
	}

	public final Tag writeChars(String s) throws IOException {
		dos.writeChars(s);
		return this;
	}

	public final Tag writeUTF(String str) throws IOException {
		dos.writeUTF(str);
		return this;
	}

	public Tag build() throws IOException {
		dos.flush();
		payload = bos.toByteArray();
		length = payload.length;
		compressionType = CompressionType.UNCOMPRESSED;
		return this;
	}

}
