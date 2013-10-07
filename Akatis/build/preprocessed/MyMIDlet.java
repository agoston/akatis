import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import java.util.*;
import java.io.*;

/**********************************************************************************
/* Misc. stuff
/**********************************************************************************/
class Stuff {
	public static final String readLine(InputStream is) throws IOException {
		byte[] buf = new byte[127];
		int index = 0;
		for (buf[index] = (byte)is.read(); buf[index] > 0 && buf[index] != '\n'; buf[++index] = (byte)is.read());
		return new String(buf, 0, index);
	}

	public static final int ln2(int in) {
		int ret = -1;
		for (; in != 0; ret++) in >>= 1;
		return ret;
	}

	public static final int sqrt(int v) {
		int root = 0;
		for (int d=0x10000000; d!=0; d>>=2) {	// root := 0.5*(root - v/root)
			int t = root+d;
			root>>=1;		// root = (prevroot + prevd) / 2
			if (t<=v) {
				v -= t;
				root += d;
			}
		}
		return root;
	}
	
	// sin+cos: tablazattal
	public final static int sin[] = {0, 1143, 2287, 3429, 4571, 5711, 6850, 7986, 9120, 10252, 11380, 12504, 13625, 14742, 15854, 16962, 18064, 19160, 20251, 21336, 22414, 23486,
								24550, 25607, 26655, 27696, 28729, 29752, 30767, 31772, 32768, 33753, 34728, 35693, 36647, 37589, 38521, 39440, 40348, 41243, 42125, 42995,
								43852, 44695, 45525, 46340, 47142, 47930, 48702, 49460, 50203, 50931, 51643, 52339, 53019, 53683, 54331, 54963, 55577, 56175, 56755, 57319,
								57864, 58393, 58903, 59395, 59870, 60326, 60763, 61183, 61583, 61965, 62328, 62672, 62997, 63302, 63589, 63856, 64103, 64331, 64540, 64729,
								64898, 65047, 65177, 65286, 65376, 65446, 65496, 65526};
								
	public static final int sin(int degree) {
		if (degree >= 180) {
			if (degree >= 270) {		// 279-359
				return -sin[359-degree];
			} else {					// 180-269
				return -sin[degree-180];
			}
		} else {
			if (degree >= 90) {			// 90-179
				return sin[179-degree];
			} else {					// 0-89
				return sin[degree];
			}
		}
	}

	public static final int cos(int degree) {
		if (degree >= 180) {
			if (degree >= 270) {		// 279-359
				return sin[degree];
			} else {					// 180-269
				return -sin[359-degree];
			}
		} else {
			if (degree >= 90) {			// 90-179
				return -sin[degree-180];
			} else {					// 0-89
				return sin[179-degree];
			}
		}
	}
}

/**********************************************************************************
/* IMAGE IO
/**********************************************************************************/

// TODO: Kesobb lecserelni olyanra, ami egy bazinagy png-t tolt be, es abbol keszit kis kepeket
// 		 Az info, hogy a kepen belul hol es mekkora datab tartozik az adott kephez, egy final static [] -ben legyen
//		 Igy gyors es szep is.

class ImageLoader {
	public static final Image get(String pos) {
		try {
			return Image.createImage(pos);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	 
	public static final Image[] getMore(String base) {
		Vector tiles = new Vector(10, 10);
		
		try {
			for (int i = 0; ; i++) {
				tiles.addElement(Image.createImage(base+(i/10)+(i%10)+".png"));
			}
		} catch (Exception e) {
		}
		Image[] tile = new Image[tiles.size()];
		tiles.copyInto(tile);
		return tile;
	}
}

/**********************************************************************************
/* EVENT
/**********************************************************************************/
class InputEvent {
	public long start;
	public int length = -1;
	public int key;
	
	public InputEvent(long _time, int _key) {
		start = _time;
		key = _key;
	}
}

class EventHandler {
	InputEvent[] ie = new InputEvent[16];  // ennyi ugyse lesz 
	int first = 0, last = 0;

	public int size() {
		return last-first;
	}
	
	public InputEvent next() {
		if (first == last) return null;
		InputEvent ret = ie[first&0xf];
		ie[first&0xf] = null;	// DEBUG
		first++;
		return ret;
	}
	
	public void add(InputEvent e) {
		if (last >= 16 && first >= 16) {
			last &= 15; first &= 15;
		}
		ie[last&0xf] = e;
		last++;
	}
	
	public InputEvent search(int key) {
		for (int i = first; i < last; i++) {
			int j = i & 0xf;
			if (ie[j].key == key && ie[j].length < 0) return ie[j];
		}
		throw new Error();
	}
	
	public void keyPressed(int key, long time) {
		InputEvent nie = new InputEvent(time, key);
		add(nie);
	}
	
	public void keyReleased(int key, long time) {
		InputEvent eie = search(key);
		eie.length = (int)(time-eie.start);
	}
}

/**********************************************************************************
/* MAIN
/**********************************************************************************/
class MyCanvas extends Canvas implements Runnable {

	static final int shipSpeed = 16;
	
	int scrX, scrY, pleft, pright, ptop, pbottom;
	int shipX, shipY, shipWidth, shipHeight;
	int pwidth, pheight, pmiddle;
	int vscrl, vscrr, vscrdelta;
	int tileHeightToDraw;
	
	long accFrameTime, lastFrameStart, actFrameStart = System.currentTimeMillis();
	int lastFrameTime;
	int bgPos;
		
	EventHandler eh = new EventHandler();
	Image[] bgImages;
	int[] bgWidth, bgHeight;
	LevelLayer[] bgLayers;

	public MyCanvas() throws IOException {
		scrX = getWidth();
		scrY = getHeight();
		pleft = 0;
		pright = scrX;
		ptop = 0;
		pbottom = scrY;
		
		pwidth = pright-pleft;
		pheight = pbottom-ptop;
		pmiddle = pleft+(pwidth/2);

		tileHeightToDraw = ((pheight+128)>>5);
		if ((pheight & 0x1f) > 0) tileHeightToDraw++;
		
		vscrl = 128 - (pwidth>>1);
		vscrr = 128 + (pwidth>>1);

		loadLevel(0);
	}

	public void run() {
		for (;;) {
			lastFrameStart = actFrameStart;
			actFrameStart = System.currentTimeMillis();
			lastFrameTime = (int)(actFrameStart-lastFrameStart);
			if (lastFrameTime > 125) lastFrameTime = 125;		// 8 fps alatt lassul a jatek
			accFrameTime += lastFrameTime;
			
			// input eventek lekezelese
			synchronized(eh) {
				int num = eh.size();
				for (int i = 0; i < num; i++) {
					InputEvent ie = eh.next();
					int length = ie.length;
					
					if (length < 0) {	// kiszamoljuk, visszarakjuk
						length = (int)(actFrameStart-ie.start);
						ie.start = actFrameStart;
						eh.add(ie);
					}
					
					length >>= 2;	// millisec -> s/256, kozelitoleg, de le van xarva
					
					switch (ie.key) {
						case Canvas.UP: movePlayer(0, -1, length); break;
						case Canvas.DOWN: movePlayer(0, 1, length); break;
						case Canvas.LEFT: movePlayer(-1, 0, length); break;
						case Canvas.RIGHT: movePlayer(1, 0, length); break;
	
						case Canvas.FIRE:
							break;
					}					
				}
			}
			
			repaint();
			serviceRepaints();
			
			try {Thread.sleep(10);} catch (Exception e) {}
		}
	}
	
	public void loadLevel(int levelNum) throws IOException {
		{	// images
			Vector v = new Vector(20,10);
			InputStream is = getClass().getResourceAsStream("/l"+levelNum+"trl");
		
			for (int i = 0; ; i++) {
				String imageFile = Stuff.readLine(is);
				if (imageFile.length() <= 1) break;
				v.addElement(Image.createImage(imageFile));
			}
			bgImages = new Image[v.size()];
			v.copyInto(bgImages);
			
			bgWidth = new int[bgImages.length];
			bgHeight = new int[bgImages.length];
			for (int i = 0; i < bgImages.length; i++) {
				bgWidth[i] = bgImages[i].getWidth();
				bgHeight[i] = bgImages[i].getHeight();
			}
		}
		
		// level layers
		{
			try {
				InputStream is = getClass().getResourceAsStream("/l"+levelNum);
				int bgNum = is.read();
				bgLayers = new LevelLayer[bgNum];
				for (int i = 0; i < bgNum; i++) {
					int len = is.read();
					if (len == -1) break;
					len += is.read()<<8;
					
					int speed = is.read();
					speed += is.read()<<8;
					
					byte[] bg = new byte[len];
					is.read(bg);
					
					bgLayers[i] = new LevelLayer(bg, speed);
				}
			} catch (Exception e) {}
		}
		
		/*background = new byte[4][];
		for (int i=0; i<background.length; i++) {
			InputStream is = getClass().getResourceAsStream("/l"+levelNum+"/bg"+i);
			int len = is.read();
			len += is.read()<<8;
			background[i] = new byte[len];
			is.read(background[i]);
		}

		{
			InputStream is = getClass().getResourceAsStream("/l"+levelNum+"/enemy");
			int len = is.read();
			len += is.read()<<8;
			enemyDesc = new byte[len];
			is.read(enemyDesc);
		}*/
		
	}
	
	public void disposeLevel() {
		bgLayers = null;
		bgImages = null;
		bgWidth = null;
		bgHeight = null;
	}
	
	void movePlayer(int x, int y, int elapsedTime) {
		if (x != 0) {
			shipX += (x*shipSpeed*elapsedTime)>>8;
			vscrdelta += x*elapsedTime;
		}
		if (y != 0) {
			shipY += (y*shipSpeed*elapsedTime)>>8;
		}
	}
	
	public void paint(Graphics g) {
		//g.fillRect(0,0,scrX,scrY);
		
		for (int i = 0; i < bgLayers.length; i++)
			bgPos = bgLayers[i].draw(g);
	}

	public void keyPressed(int i) {
		long time = System.currentTimeMillis();
		i = getGameAction(i);
		switch (i) {
			default:
				synchronized(eh) {
					eh.keyPressed(i, time);
				}
		}
	}
	
	public void keyReleased(int i) {
		long time = System.currentTimeMillis();
		i = getGameAction(i);
		switch (i) {
			default:
				synchronized(eh) {
					eh.keyReleased(i, time);
				}
		}
	}


/**********************************************************************************
/* ACTORS
/**********************************************************************************/

class Actor {
	
}

class Enemy extends Actor {
	
}

class Ship extends Actor {
	
}

/**********************************************************************************
/* LAYERS
/**********************************************************************************/
// TODO: 256pxnel szelesebb kepernyonel nem muxik!!! Oda majd egy masik, mondjuk BigLevelLayer class-t kell csinalni
// 		 (vagy ezt kijavitani, de inkabb az utobbi)

class LevelLayer {
	final byte[] bg;
	public int speed;
	
	public LevelLayer(byte[] _bg, int _speed) {
		speed = _speed;
		bg = _bg;

		
		//System.out.println("P: "+pleft+", "+ptop+", "+pwidth+", "+pheight);
		//System.out.println("TileHeightToDraw: "+tileHeightToDraw);
	}
	
	public int draw(Graphics g) {
		// clip
		//g.setClip(pleft, ptop, pwidth, pheight);
		
		// bgpos ujrakalkulalasa
		int bgPos = (int)((accFrameTime*speed)>>16);
		
		int bgPosMod = bgPos&0x1f;
		int bgPosDiv = bgPos>>5;
		int vscrleft = vscrl + ((vscrdelta*speed)>>16);
		int vscrright = vscrr + ((vscrdelta*speed)>>16);
		
		// kirajzolas
		int bgIndex = bgPosDiv<<4;
		for (int i = 1; i <= tileHeightToDraw; i++) {
			for (int j = 0; j < 16; j++) {
				int actImIndex = bg[bgIndex++];
				if (actImIndex < 0 || actImIndex >= bgImages.length) continue;
				
				// Y irany check
				int topPos = i<<5;
				if (topPos-bgHeight[actImIndex] > pheight+bgPosMod) continue;

				// X irany check
				int leftPos = j << 4;
				if (leftPos >= vscrright || bgWidth[actImIndex]+leftPos < vscrleft) continue;
				
				g.drawImage(bgImages[actImIndex], pleft+leftPos-vscrleft, pbottom + bgPosMod - topPos, Graphics.TOP|Graphics.LEFT);
			}
		}
		
		return bgPos;
	}
}

class ActorLayer {
	
	public boolean draw(Graphics g) {		// true, ha megszunt a layer (alias vege a palyanak)
		return false;
	}
	
}

class MessageLayer {

	public boolean draw(Graphics g) {		// true, ha megszunt a layer (alias vege az uzenetnek)
		return false;
	}

}

}

public class MyMIDlet extends MIDlet {
	MyCanvas canvas;

	public MyMIDlet() {
	}
		
	public void startApp() {
		try {
			canvas = new MyCanvas();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Display.getDisplay(this).setCurrent(canvas);

		Thread t = new Thread(canvas);
		t.start();
	}
	
	public void pauseApp() {
	}
	
	public void destroyApp(boolean unconditional) {
	}
}
