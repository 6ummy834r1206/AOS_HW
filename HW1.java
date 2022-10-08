import java.util.Random;
import java.util.Scanner;

public class Main {
	static int diskW = 0, interruptN = 0;

	public static void main(String[] args) {
		int frameN = 0, tCase = -1, t2Num = 0, t2Start = 0;
		int func = 0, frameFlag = 0, pagefaultN = 0;
		int t3Num = 0, t3Start = 0;
		int str[] = new int[100000];// Reference string
		// int str[] = { 1, 2, 3, 4, 1, 2, 5, 1, 2, 3, 4, 5 };
		// int str[] = { 7, 9, 1, 2, 9, 3, 9, 4, 2, 3, 9, 3, 2, 1, 2, 9, 1, 7, 9, 1 };
		int frame[];
		int ESC[][];
		Random ranStr = new Random();
		Scanner scan = new Scanner(System.in);
		System.out.print("�п�J����Case: ");
		tCase = scan.nextInt();
		switch (tCase) { // �M�wreference String
		case 1:
			for (int i = 0; i < str.length; i++) {
				str[i] = ranStr.nextInt(500) + 1;
			}
			break;
		case 2:
			int storeFlag = 0;
			double locality = 0;
			while(str[str.length-1] == 0) {
				//1/20~1/10 �۷��0.05~0.10
				//�üƨ�5~10�A/100
				locality = (ranStr.nextDouble()*5+5)/100;
				t2Num = (int)(500 * locality);
				t2Start = ranStr.nextInt(500 - t2Num) + 1;
				for (int i = 0; i < t2Num && str[str.length-1] == 0; i++) {
					str[i+storeFlag] = t2Start + i;
				}
				storeFlag = storeFlag + t2Num;
			}
			
			break;
		case 3: // design by myself
			// �]������frame�̦h100�ӡA�p�Gpage���ƩʷU���Aoptimal��pageFault���ƷU��
			// �ҥH�D120�ӳs��string����
			// �H������̭���string
			t3Num = 120;
			t3Start = ranStr.nextInt(500-t3Num) + 1;
			for (int i = 0; i < str.length; i++) {
				str[i] = ranStr.nextInt(t3Num) + t3Start;
			}
			break;
		default:// Case�u��123�Ӥw
			System.out.println("���ո�Ƥ��s�b�A�Э���");
			System.exit(0);
			;
		}
		System.out.print("�п�J�n�ϥΪ�Function(1~4): ");
		// (1) FIFO algorithm(2) Optimal algorithm
		// (3) Enhanced second-chance algorithm(4)design by myself
		func = scan.nextInt();
		for (int frameL = 1; frameL <= 10; frameL++) {
			frameN = frameL * 10;
			frameFlag = 0;
			pagefaultN = 0;
			interruptN = 0;
			diskW = 0;
			frame = new int[frameN];// [frame]
			switch (func) {
			case 1: // FIFO
				L: for (int i = 0; i < str.length; i++) {
					for (int j = 0; j < frame.length; j++) { // �ˬdpage�S���Aframe�̭�
						if (frame[j] == str[i])
							continue L; // ���N���U��Reference string�ˬd
					}
					pagefaultN++; // �o��pageFault
					interruptN++; // pageFault�ɡAinterrupt++
					if (frame[frame.length - 1] != 0) { // page�S���bframe�̭��Bframe�w�s�񧹳̫�@��
						dirtyBitandDiskW(); // �o��pagefault�ɡA�ˬdvictim frame�H�����t��dirtybit�Y��1�A�N��page�Q��L�Adiskwrite�W�[�@��
						frame[frameFlag] = str[i];
					} else {
						frame[frameFlag] = str[i];
					}
					frameFlag++;
					if (frameFlag >= frame.length)
						frameFlag %= frame.length;
				}
				break;
			case 2: // Optimal
				int frameUse[] = new int[frameN]; // ����frame ����|���|�Ψ�
				int frameMax = 0;
				M: for (int i = 0; i < str.length; i++) {
					frameMax = 0;
					for (int j = 0; j < frame.length; j++) { // �ˬdpage�S���Aframe�̭�
						if (frame[j] == str[i])
							continue M; // ���N���U��Reference string�ˬd
					}
					pagefaultN++; // �o��pagefault
					interruptN++; // pagefault��,interrupt++
					if (frame[frame.length - 1] != 0) {// �Yframe���A�h�}�l�D��victim frame
						for (int k = 0; k < frame.length; k++) { // �̷Ӥ���̥��Ψ쪺�����s��
							for (int l = i + 1; l < str.length; l++) {
								if (frame[k] == str[l]) { // ���̪�ϥιL��page
									frameUse[k] = l; // �����Ҧb��m
									if (frameMax < l) {
										frameMax = l;
									}
									break;// ���U��frame�s��
								} else if (l + 1 >= str.length) { // ���n�����٨S���
									frameFlag = k; // ���N�O�A��
									dirtyBitandDiskW(); // �P�_�Y�N�g�J��page�O�_�ק�L
									frame[frameFlag] = str[i];
									continue M;
								}
							}
						}
						for (int m = 0; m < frame.length; m++) { // ���̱ߥΨ쪺page
							if (frame[m] == str[frameMax]) {
								frameFlag = m;
							}
						}
						dirtyBitandDiskW(); // �P�_�Y�N�g�J��page�O�_�ק�L
						frame[frameFlag] = str[i];
					} else { // �̤@�}�l�A����frame��
						frame[frameFlag] = str[i];
						frameFlag++;
					}
					if (frameFlag >= frame.length)
						frameFlag %= frame.length;
				}
				break;
			case 3: // ESC
				ESC = new int[frameN][2]; // [�ĴX��frame][�Ĥ@�Ӧs�̪�Q�ϥιL,�ĤG�ӦsDirtybit�A1�N�O���Q��L]
				int pointer = 0;
				O: for (int i = 0; i < str.length; i++) {
					// Ūreference string
					for (int j = 0; j < frame.length; j++) { // �ˬdpage�S���Aframe�̭�
						pointer = (j + frameFlag) % frame.length;
						if (frame[pointer] == str[i]) {
							for (int m = pointer - 1;; m--) { // ������L��reference bits �� 0
								if (m < 0) {
									m = frame.length - 1;
								}
								ESC[m][0] = 0;
								if (m == frameFlag) {
									break;
								}
							}
							ESC[pointer][0] = 1; // �o��frame�̪񦳳Q�ϥιL
							if (frame[frame.length - 1] != 0) {
								frameFlag = pointer + 1;// frameFlag�аO��m,frameFlag���U�@�ӧP�_
							}
							if (frameFlag >= frame.length)
								frameFlag %= frame.length;
							continue O; // ���o��page�N���U��Reference string�ˬd
						}
					}
					if (frame[frame.length - 1] == 0) { // frame�٪ŪŪ�
						pagefaultN++; // �o��pagefault
						interruptN++; // pagefault��,interrupt++
						frame[frameFlag] = str[i];
						ESC[frameFlag][1] = ranStr.nextInt(2); // �H���ᤩdirty bit�A�K�󤧫�P�_��page�O�_���ק�L
						frameFlag++;
						if (frameFlag >= frame.length)
							frameFlag %= frame.length;
					} else { // frame���F
						for (int k = 0; k < frame.length; k++) {
							pointer = (frameFlag + k) % frame.length;
							if (ESC[pointer][0] == 0 && ESC[pointer][1] == 0) { // (0,0) �u���������κޫ᭱
								for (int m = pointer - 1;; m--) { // ������L��reference bits �� 0
									if (m < 0) {
										m = frame.length - 1;
									}
									ESC[m][0] = 0;
									if (m == frameFlag) {
										break;
									}
								}
								pagefaultN++; // �o��pagefault
								interruptN++; // pagefault��,interrupt++
								frame[pointer] = str[i];
								ESC[pointer][0] = 1;
								ESC[pointer][1] = ranStr.nextInt(2);
								frameFlag = pointer + 1;
								if (frameFlag >= frame.length)
									frameFlag %= frame.length;
								continue O;
							}
						}
						// �٨S���victim frame�A�S��(0,0)��
						for (int l = 0; l < frame.length; l++) {
							pointer = (frameFlag + l) % frame.length;
							if (ESC[pointer][0] == 0 && ESC[pointer][1] == 1) { // (0,1) �u���������κޫ᭱
								for (int n = 0; n < frame.length; n++) {
									// �ѩ��(0,0)�ɡA���䤣��A�b�o����(0,1)���A�ҥH����Ҧ���reference bit �]0
									ESC[n][0] = 0;
								}
								pagefaultN++; // �o��pagefault
								interruptN++; // pagefault��,interrupt++
								frame[pointer] = str[i];
								diskW++;// page���ק�L
								interruptN++; // diskW�o��interrupt
								ESC[pointer][0] = 1; // ��o��page��reference bit �]��1
								ESC[pointer][1] = ranStr.nextInt(2);
								frameFlag = pointer + 1;
								if (frameFlag >= frame.length)
									frameFlag %= frame.length;
								continue O;
							}
						}
						for (int l = 0; l < frame.length; l++) {
							pointer = (frameFlag + l) % frame.length;
							if (ESC[pointer][0] == 1 && ESC[pointer][1] == 0) { // (0,1) �u���������κޫ᭱
								for (int n = 0; n < frame.length; n++) {
									// �ѩ��(0,0)�ɡA���䤣��A�b�o����(0,1)���A�ҥH����Ҧ���reference bit �]0
									ESC[n][0] = 0;
								}
								pagefaultN++; // �o��pagefault
								interruptN++; // pagefault��,interrupt++
								frame[pointer] = str[i];
								diskW++;// page���ק�L
								interruptN++; // diskW�o��interrupt
								ESC[pointer][0] = 1; // ��o��page��reference bit �]��1
								ESC[pointer][1] = ranStr.nextInt(2);
								frameFlag = pointer + 1;
								if (frameFlag >= frame.length)
									frameFlag %= frame.length;
								continue O;
							}
						}
						for (int l = 0; l < frame.length; l++) {
							pointer = (frameFlag + l) % frame.length;
							if (ESC[pointer][0] == 1 && ESC[pointer][1] == 1) { // (1,1) �u������
								for (int n = 0; n < frame.length; n++) {
									// ��Ҧ���reference bit �]0
									ESC[n][0] = 0;
								}
								pagefaultN++; // �o��pagefault
								interruptN++; // pagefault��,interrupt++
								frame[pointer] = str[i];
								diskW++;// page���ק�L
								interruptN++; // diskW�o��interrupt
								ESC[pointer][0] = 1; // ��o��page��reference bit �]��1
								ESC[pointer][1] = ranStr.nextInt(2);
								frameFlag = pointer + 1;
								if (frameFlag >= frame.length)
									frameFlag %= frame.length;
								continue O;
							}
						}
					}
				}
				break;
			case 4: // design by myself
				M: for (int i = 0; i < str.length; i++) {
					for (int j = 0; j < frame.length; j++) { // �ˬdpage�S���Aframe�̭�
						if (frame[j] == str[i])
							continue M; // ���N���U��Reference string�ˬd
					}
					pagefaultN++; // �o��pagefault
					interruptN++; // pagefault��,interrupt++
					if (frame[frame.length - 1] != 0) {// �Yframe���A�h�}�l�D��victim frame
						Q:for (int k = 0; k < frame.length; k++) {
							for (int l = i + 1; l < i+frame.length && l < str.length; l++) {
								if (frame[k] == str[l]) {
									continue Q;
								}else if (l+1 == i+frame.length) {
									frameFlag = k;
								}
							}
						}
						dirtyBitandDiskW(); // �P�_�Y�N�g�J��page�O�_�ק�L
						frame[frameFlag] = str[i];
					} else { // �̤@�}�lframe�ܪšA����frame��
						frame[frameFlag] = str[i];
						frameFlag++;
					}
					if (frameFlag >= frame.length)
						frameFlag %= frame.length;
				}
				break;
			}
			/*System.out.println("Frame number: " + frameN);
			System.out.println("pagefault: " + pagefaultN + " interrupt: " + interruptN + " diskwrite: " + diskW);
			*/
			System.out.println(pagefaultN + "," + interruptN + "," + diskW);
		}

	}

	public static void dirtyBitandDiskW() {
		Random ranBit = new Random();
		if (ranBit.nextBoolean()) {
			diskW++;
			interruptN++; // �bdisk�W���m��i�h
		}
	}
}
