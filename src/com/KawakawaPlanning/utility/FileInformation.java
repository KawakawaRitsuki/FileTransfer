package com.KawakawaPlanning.utility;

import java.io.File;
import java.util.Locale;

/**
 * �t�@�C�����������N���X�ł��B�p�b�P�[�W���N���X�B
 */
class FileInformation implements Comparable<FileInformation> {
	/**
	 * �t�@�C���B
	 */
	private File file;
	
	/**
	 * �R���X�g���N�^�B
	 * @param file �t�@�C��
	 */
	public FileInformation(File file) {
		this.file = file;
	}

	@Override
	public int compareTo(FileInformation opponent) {
		if(this.file.isDirectory() && ! opponent.file.isDirectory()) {
			return -1;
		} else if(! this.file.isDirectory() && opponent.file.isDirectory()) {
			return 1;
		} else {
			return this.file.getName().toLowerCase(Locale.US).compareTo(opponent.file.getName().toLowerCase(Locale.US));
		}
	}
	
	/**
	 * �t�@�C����Ԃ��܂��B
	 * @return �t�@�C��
	 */
	public File getFile() {
		return this.file;
	}
}
