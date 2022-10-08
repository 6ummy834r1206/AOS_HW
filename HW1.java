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
		System.out.print("請輸入測試Case: ");
		tCase = scan.nextInt();
		switch (tCase) { // 決定reference String
		case 1:
			for (int i = 0; i < str.length; i++) {
				str[i] = ranStr.nextInt(500) + 1;
			}
			break;
		case 2:
			int storeFlag = 0;
			double locality = 0;
			while(str[str.length-1] == 0) {
				//1/20~1/10 相當於0.05~0.10
				//亂數取5~10再/100
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
			// 因為實驗frame最多100個，如果page重複性愈高，optimal的pageFault次數愈少
			// 所以挑120個連續的string之後
			// 隨機抓取裡面的string
			t3Num = 120;
			t3Start = ranStr.nextInt(500-t3Num) + 1;
			for (int i = 0; i < str.length; i++) {
				str[i] = ranStr.nextInt(t3Num) + t3Start;
			}
			break;
		default:// Case只有123而已
			System.out.println("測試資料不存在，請重試");
			System.exit(0);
			;
		}
		System.out.print("請輸入要使用的Function(1~4): ");
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
					for (int j = 0; j < frame.length; j++) { // 檢查page沒有再frame裡面
						if (frame[j] == str[i])
							continue L; // 有就往下個Reference string檢查
					}
					pagefaultN++; // 發生pageFault
					interruptN++; // pageFault時，interrupt++
					if (frame[frame.length - 1] != 0) { // page沒有在frame裡面且frame已存放完最後一格
						dirtyBitandDiskW(); // 發生pagefault時，檢查victim frame隨機分配的dirtybit若為1，代表page被改過，diskwrite增加一次
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
				int frameUse[] = new int[frameN]; // 紀錄frame 之後會不會用到
				int frameMax = 0;
				M: for (int i = 0; i < str.length; i++) {
					frameMax = 0;
					for (int j = 0; j < frame.length; j++) { // 檢查page沒有再frame裡面
						if (frame[j] == str[i])
							continue M; // 有就往下個Reference string檢查
					}
					pagefaultN++; // 發生pagefault
					interruptN++; // pagefault時,interrupt++
					if (frame[frame.length - 1] != 0) {// 若frame滿，則開始挑選victim frame
						for (int k = 0; k < frame.length; k++) { // 依照之後最先用到的給予編號
							for (int l = i + 1; l < str.length; l++) {
								if (frame[k] == str[l]) { // 找到最近使用過的page
									frameUse[k] = l; // 紀錄所在位置
									if (frameMax < l) {
										frameMax = l;
									}
									break;// 往下個frame編號
								} else if (l + 1 >= str.length) { // 都要結束還沒找到
									frameFlag = k; // 那就是你啦
									dirtyBitandDiskW(); // 判斷即將寫入的page是否修改過
									frame[frameFlag] = str[i];
									continue M;
								}
							}
						}
						for (int m = 0; m < frame.length; m++) { // 找到最晚用到的page
							if (frame[m] == str[frameMax]) {
								frameFlag = m;
							}
						}
						dirtyBitandDiskW(); // 判斷即將寫入的page是否修改過
						frame[frameFlag] = str[i];
					} else { // 最一開始，直到frame滿
						frame[frameFlag] = str[i];
						frameFlag++;
					}
					if (frameFlag >= frame.length)
						frameFlag %= frame.length;
				}
				break;
			case 3: // ESC
				ESC = new int[frameN][2]; // [第幾個frame][第一個存最近被使用過,第二個存Dirtybit，1就是有被改過]
				int pointer = 0;
				O: for (int i = 0; i < str.length; i++) {
					// 讀reference string
					for (int j = 0; j < frame.length; j++) { // 檢查page沒有再frame裡面
						pointer = (j + frameFlag) % frame.length;
						if (frame[pointer] == str[i]) {
							for (int m = pointer - 1;; m--) { // 把當初路過的reference bits 改 0
								if (m < 0) {
									m = frame.length - 1;
								}
								ESC[m][0] = 0;
								if (m == frameFlag) {
									break;
								}
							}
							ESC[pointer][0] = 1; // 這個frame最近有被使用過
							if (frame[frame.length - 1] != 0) {
								frameFlag = pointer + 1;// frameFlag標記位置,frameFlag往下一個判斷
							}
							if (frameFlag >= frame.length)
								frameFlag %= frame.length;
							continue O; // 有這個page就往下個Reference string檢查
						}
					}
					if (frame[frame.length - 1] == 0) { // frame還空空的
						pagefaultN++; // 發生pagefault
						interruptN++; // pagefault時,interrupt++
						frame[frameFlag] = str[i];
						ESC[frameFlag][1] = ranStr.nextInt(2); // 隨機賦予dirty bit，便於之後判斷該page是否為修改過
						frameFlag++;
						if (frameFlag >= frame.length)
							frameFlag %= frame.length;
					} else { // frame滿了
						for (int k = 0; k < frame.length; k++) {
							pointer = (frameFlag + k) % frame.length;
							if (ESC[pointer][0] == 0 && ESC[pointer][1] == 0) { // (0,0) 優先替換不用管後面
								for (int m = pointer - 1;; m--) { // 把當初路過的reference bits 改 0
									if (m < 0) {
										m = frame.length - 1;
									}
									ESC[m][0] = 0;
									if (m == frameFlag) {
										break;
									}
								}
								pagefaultN++; // 發生pagefault
								interruptN++; // pagefault時,interrupt++
								frame[pointer] = str[i];
								ESC[pointer][0] = 1;
								ESC[pointer][1] = ranStr.nextInt(2);
								frameFlag = pointer + 1;
								if (frameFlag >= frame.length)
									frameFlag %= frame.length;
								continue O;
							}
						}
						// 還沒找到victim frame，沒有(0,0)的
						for (int l = 0; l < frame.length; l++) {
							pointer = (frameFlag + l) % frame.length;
							if (ESC[pointer][0] == 0 && ESC[pointer][1] == 1) { // (0,1) 優先替換不用管後面
								for (int n = 0; n < frame.length; n++) {
									// 由於找(0,0)時，都找不到，在這邊找到(0,1)的，所以先把所有的reference bit 設0
									ESC[n][0] = 0;
								}
								pagefaultN++; // 發生pagefault
								interruptN++; // pagefault時,interrupt++
								frame[pointer] = str[i];
								diskW++;// page有修改過
								interruptN++; // diskW發生interrupt
								ESC[pointer][0] = 1; // 把這個page的reference bit 設成1
								ESC[pointer][1] = ranStr.nextInt(2);
								frameFlag = pointer + 1;
								if (frameFlag >= frame.length)
									frameFlag %= frame.length;
								continue O;
							}
						}
						for (int l = 0; l < frame.length; l++) {
							pointer = (frameFlag + l) % frame.length;
							if (ESC[pointer][0] == 1 && ESC[pointer][1] == 0) { // (0,1) 優先替換不用管後面
								for (int n = 0; n < frame.length; n++) {
									// 由於找(0,0)時，都找不到，在這邊找到(0,1)的，所以先把所有的reference bit 設0
									ESC[n][0] = 0;
								}
								pagefaultN++; // 發生pagefault
								interruptN++; // pagefault時,interrupt++
								frame[pointer] = str[i];
								diskW++;// page有修改過
								interruptN++; // diskW發生interrupt
								ESC[pointer][0] = 1; // 把這個page的reference bit 設成1
								ESC[pointer][1] = ranStr.nextInt(2);
								frameFlag = pointer + 1;
								if (frameFlag >= frame.length)
									frameFlag %= frame.length;
								continue O;
							}
						}
						for (int l = 0; l < frame.length; l++) {
							pointer = (frameFlag + l) % frame.length;
							if (ESC[pointer][0] == 1 && ESC[pointer][1] == 1) { // (1,1) 優先替換
								for (int n = 0; n < frame.length; n++) {
									// 把所有的reference bit 設0
									ESC[n][0] = 0;
								}
								pagefaultN++; // 發生pagefault
								interruptN++; // pagefault時,interrupt++
								frame[pointer] = str[i];
								diskW++;// page有修改過
								interruptN++; // diskW發生interrupt
								ESC[pointer][0] = 1; // 把這個page的reference bit 設成1
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
					for (int j = 0; j < frame.length; j++) { // 檢查page沒有再frame裡面
						if (frame[j] == str[i])
							continue M; // 有就往下個Reference string檢查
					}
					pagefaultN++; // 發生pagefault
					interruptN++; // pagefault時,interrupt++
					if (frame[frame.length - 1] != 0) {// 若frame滿，則開始挑選victim frame
						Q:for (int k = 0; k < frame.length; k++) {
							for (int l = i + 1; l < i+frame.length && l < str.length; l++) {
								if (frame[k] == str[l]) {
									continue Q;
								}else if (l+1 == i+frame.length) {
									frameFlag = k;
								}
							}
						}
						dirtyBitandDiskW(); // 判斷即將寫入的page是否修改過
						frame[frameFlag] = str[i];
					} else { // 最一開始frame很空，直到frame滿
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
			interruptN++; // 在disk上找位置放進去
		}
	}
}
