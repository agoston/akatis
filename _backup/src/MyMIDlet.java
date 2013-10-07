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
/* MAIN
/**********************************************************************************/
class MyCanvas extends Canvas implements Runnable {

	static final int shipSpeed = 16;
	static final int maxBGShift = 16;
	
	int scrX, scrY, pleft, pright, ptop, pbottom;
	int shipX, shipY, shipWidth, shipHeight;
	int pwidth, pheight, pmiddle;
	int vscrl, vscrr, vscrdelta, maxvscrdelta, maxbgspeed, enspeed;
	int tileHeightToDraw;
	
	long accFrameTime, lastFrameStart, actFrameStart;
	int lastFrameTime;
	int accbgpos, enbgpos;
		
	Image[] bgImages;
	int[] bgWidth, bgHeight;
	Drawable[] bgLayers = new Drawable[0];

	Enemy[] enemies = new Enemy[0];
	Image[] enemyImages = new Image[16];
	int[] enHeight = new int[16];
	int[] enWidth = new int[16];
	int enBegin = 0;
	
	Image[] bulletImages = new Image[16];
	int[] bulletHeight = new int[16];
	int[] bulletWidth = new int[16];
	
	Path[] path;
	
	public MyCanvas() {
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
		
		vscrl = (pwidth>>1) - 96;
		vscrr = (pwidth>>1) + 96;
	}

	public void run() {
		loadLevel(0);

		for (;;) {
			// idoszamitas
			lastFrameStart = actFrameStart;
			actFrameStart = System.currentTimeMillis();
			lastFrameTime = (int)(actFrameStart-lastFrameStart);
			if (lastFrameTime > 125) lastFrameTime = 125;		// 8 fps alatt lassul a jatek
			accFrameTime += lastFrameTime>>2;
			
			// input eventek lekezelese
			synchronized(ehie) {
				int num = ehSize();
				for (int i = 0; i < num; i++) {
					InputEvent ie = ehNext();
					int length = ie.length;
					
					if (length < 0) {	// kiszamoljuk, visszarakjuk
						length = (int)(actFrameStart-ie.start);
						ie.start = actFrameStart;
						ehAdd(ie);
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
	
	public void loadLevel(int levelNum) {
		// reinit values
		accFrameTime = 0;
		accbgpos = 0;
		actFrameStart = System.currentTimeMillis();
		enBegin = 0;
		
		try {
			// images
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
		
			// pathes
			if (path == null) {
				is = getClass().getResourceAsStream("/p");
				int pNum = is.read();
				path = new Path[pNum];
				for (int i = 0; i < pNum; i++) {
					int len = is.read();
					Path actpath = new Path(len);
					path[i] = actpath;
					for (int j = 0; j < len; j++) {
						actpath.x[j] = is.read();
						actpath.y[j] = is.read();
						actpath.speed[j] = is.read();
						actpath.speed[j] += is.read()<<8;
						actpath.wait[j] = is.read();
						actpath.wait[j] += is.read()<<8;
					}
				}
			}
			
			// level layers
			is = getClass().getResourceAsStream("/l"+levelNum);
			int bgNum = is.read();
			bgLayers = new Drawable[bgNum];
			for (int i = 0; i < bgNum; i++) {
				int len = is.read();
				if (len == -1) break;
				len += is.read()<<8;

				int speed = is.read();
				speed += is.read()<<8;
				if (speed > maxbgspeed) maxbgspeed = speed;
				enspeed = speed;
				
				int stretch = is.read();

				byte[] bg = new byte[len];
				is.read(bg);

				switch (stretch) {
					case 0:
						bgLayers[i] = new CenteredLevelLayer(bg, speed); break;
					case 1:
						bgLayers[i] = new RepeatedLevelLayer(bg, speed); break;
					case 2:
						bgLayers[i] = new JustifiedLevelLayer(bg, speed); break;
				}
			}
			maxvscrdelta = (maxBGShift<<16)/maxbgspeed;
			// propagaljuk - eleg szerencsetlen, de meg mindig a legkisebb eroforrasugenyu
			for (int i = 0; i < bgLayers.length; i++)
				if (bgLayers[i] instanceof JustifiedLevelLayer)
					((JustifiedLevelLayer)bgLayers[i]).postInit();
			
			// enemies
			int enNum = is.read();
			enemies = new Enemy[enNum];
			for (int i = 0; i < enNum; i++) {
				int type = is.read();
				int bgPos = is.read();
				bgPos += is.read()<<8;

				int path = is.read();
				int dx = is.read();
				int dy = is.read();
				int hp = is.read();
				hp += is.read()<<8;
				int weapon = is.read();
				int point = is.read();
				point += is.read()<<8;

				enemies[i] = new Enemy(type, bgPos, path, dx, dy, hp, weapon, point);
				
				// images
				if (enemyImages[type] == null) {
					enemyImages[type] = Image.createImage("/en/e"+(type/10)+(type%10)+".png");
					enWidth[type] = enemyImages[type].getWidth();
					enHeight[type] = enemyImages[type].getHeight();
				}
				
				// bullets
				if (bulletImages[weapon] == null) {
					bulletImages[weapon] = Image.createImage("/en/b"+(weapon/10)+(weapon%10)+".png");
					bulletWidth[weapon] = bulletImages[weapon].getWidth();
					bulletHeight[weapon] = bulletImages[weapon].getHeight();
				}

			}
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void disposeLevel() {
		bgLayers = null;
		bgImages = null;
		bgWidth = null;
		bgHeight = null;
		enemies = null;
		enemyImages = new Image[16];
		bulletImages = new Image[16];
	}
	
	void movePlayer(int x, int y, int elapsedTime) {
		if (x != 0) {
			shipX += (x*shipSpeed*elapsedTime)>>8;
			vscrdelta += x*elapsedTime;
			if (vscrdelta > maxvscrdelta) vscrdelta = maxvscrdelta;
			if (vscrdelta < -maxvscrdelta) vscrdelta = -maxvscrdelta;
		}
		if (y != 0) {
			shipY += (y*shipSpeed*elapsedTime)>>8;
		}
	}
	
	public void paint(Graphics g) {
		// BG
		g.setColor(0, 255, 0); g.fillRect(0, 0, scrX, scrY);
		
		// layers
		for (int i = 0; i < bgLayers.length; i++)
			bgLayers[i].draw(g);
		
		// enemy
		g.setClip(pleft, ptop, pwidth, pheight);
		enbgpos = (int)((accFrameTime*maxbgspeed)>>16)+(accbgpos>>8)+pheight;
		
		for (int i = enBegin; i < enemies.length; i++) {
			if (enemies[i] == null) {
				if (i == enBegin) enBegin++;
				continue;
			}
			
			Enemy en = enemies[i];
			if (en.bgPos <= enbgpos+enHeight[en.type]) {	// aktiv
				// mozgat
				//en.move();
				
				// kirajzol
				int enshift = (((vscrdelta*enspeed)+32768)>>16);
				g.drawImage(enemyImages[en.type], en.dx+enshift, enbgpos - en.bgPos, Graphics.TOP|Graphics.LEFT);
				
			} else {
				break;
			}
		}
		
	}


/**********************************************************************************
/* EVENT
/**********************************************************************************/
	class InputEvent {
		public long start;
		public int length = -1;
		public int key;
	}

	InputEvent[] ehie = new InputEvent[16];  // ennyi ugyse lesz 
	int ehFirst = 0, ehLast = 0;

	public int ehSize() {
		return ehLast-ehFirst;
	}
	
	public InputEvent ehNext() {
		if (ehFirst == ehLast) return null;
		InputEvent ret = ehie[ehFirst&0xf];
		ehie[ehFirst&0xf] = null;	// DEBUG
		ehFirst++;
		return ret;
	}
	
	public void ehAdd(InputEvent e) {
		if (ehLast >= 16 && ehFirst >= 16) {
			ehLast &= 15; ehFirst &= 15;
		}
		ehie[ehLast&0xf] = e;
		ehLast++;
	}
	
	public InputEvent ehSearch(int key) {
		for (int i = ehFirst; i < ehLast; i++) {
			int j = i & 0xf;
			if (ehie[j].key == key && ehie[j].length < 0) return ehie[j];
		}
		throw new Error();
	}
	
	public void keyPressed(int i) {
		long time = System.currentTimeMillis();
		int key = getGameAction(i);
		synchronized(ehie) {
			InputEvent nie = new InputEvent();
			nie.start = time;
			nie.key = key;
			ehAdd(nie);
		}
	}
	
	public void keyReleased(int i) {
		long time = System.currentTimeMillis();
		int key = getGameAction(i);
		synchronized(ehie) {
			InputEvent eie = ehSearch(key);
			eie.length = (int)(time-eie.start);
		}
	}


/**********************************************************************************
/* PATH
/**********************************************************************************/
class Path {
	public final int[] x, y, speed, wait;
	
	public Path(int numPoints) {
		x = new int[numPoints];
		y = new int[numPoints];
		speed = new int[numPoints];
		wait = new int[numPoints];
	}
}

/**********************************************************************************
/* ACTORS
/**********************************************************************************/

class Actor {
	
}

class Enemy {
	public int bgPos, hp, type, dx, dy, weapon, point;
	Path p;
	
	public Enemy(int _type, int _bgPos, int _path, int _dx, int _dy, int _hp, int _weapon, int _point) {
		type = _type;
		bgPos = _bgPos;
		dx = _dx;
		dy = _dy;
		hp = _hp;
		weapon = _weapon;
		point = _point;
		p = path[_path];
	}	
}	

/**********************************************************************************
/* LAYERS
/**********************************************************************************/
// TODO: bg hossz check (ha elerte a palya veget, ne dobja el magat!)
//		 (nem biztos! ha jol van megcsinalva a palya vege, gyorsithat az elhagyasa!)
interface Drawable {
	void draw(Graphics g);
}

class CenteredLevelLayer implements Drawable {
	final byte[] bg;
	public int speed;
	
	public CenteredLevelLayer(byte[] _bg, int _speed) {
		speed = _speed;
		bg = _bg;
	}
	
	public void draw(Graphics g) {
		// clip
		g.setClip(pleft, ptop, pwidth, pheight);
		
		// bgpos ujrakalkulalasa
		int bgPos = (int)((accFrameTime*speed)>>16) + (accbgpos>>8);
		
		int bgPosMod = bgPos&0x1f;
		int bgPosDiv = bgPos>>5;
		int vscrleft = vscrl + (((vscrdelta*speed)+32768)>>16);
		
		// kirajzolas
		int bgIndex = bgPosDiv*12;
		for (int i = 1; i <= tileHeightToDraw; i++) {
			int topPos = i<<5;

			for (int j = 0; j < 12; j++) {
				int actImIndex = bg[bgIndex++];
				if (actImIndex < 0 || actImIndex >= bgImages.length) continue;
				
				// Y irany check
				if (topPos-bgHeight[actImIndex] > pheight+bgPosMod) continue;

				// X irany check
				int leftPos = (j << 4)+vscrleft;
				if (leftPos >= pright || bgWidth[actImIndex]+leftPos < pleft) continue;   /** modified for editor */
				
				g.drawImage(bgImages[actImIndex], pleft+leftPos, pbottom + bgPosMod - topPos, Graphics.TOP|Graphics.LEFT);
			}
		}
	}
}

class RepeatedLevelLayer implements Drawable {
	final byte[] bg;
	public int speed, tileWidthToDraw, startTile, endTile;
	
	public RepeatedLevelLayer(byte[] _bg, int _speed) {
		speed = _speed;
		bg = _bg;

		tileWidthToDraw = pwidth>>4;
		if ((pwidth&0xf) != 0) tileWidthToDraw++;
			
		startTile = 6-(tileWidthToDraw>>1);
		endTile = 6+(tileWidthToDraw>>1);
	}
	
	public void draw(Graphics g) {
		// clip
		g.setClip(pleft, ptop, pwidth, pheight);
		
		// bgpos ujrakalkulalasa
		int bgPos = (int)((accFrameTime*speed)>>16) + (accbgpos>>8);
		
		int bgPosMod = bgPos&0x1f;
		int bgPosDiv = bgPos>>5;
		int vscrleft = vscrl + (((vscrdelta*speed)+32768)>>16);
		
		// kirajzolas
		for (int i = 1; i <= tileHeightToDraw; i++) {
			int bgIndex = (bgPosDiv+i-1)*12;
			int topPos = i<<5;

			for (int j = -7; j < endTile; j++) {
				int actInd = j;
				while (actInd < 0) actInd += 6;
				while (actInd >= 12) actInd -= 6;
				int actImIndex = bg[bgIndex+actInd];
				if (actImIndex < 0 || actImIndex >= bgImages.length) continue;

				// Y irany check
				if (topPos-bgHeight[actImIndex] > pheight+bgPosMod) continue;

				// X irany check
				int leftPos = (j << 4)+vscrleft;
				if (leftPos >= pright || bgWidth[actImIndex]+leftPos < pleft) continue;

				g.drawImage(bgImages[actImIndex], pleft+leftPos, pbottom + bgPosMod - topPos, Graphics.TOP|Graphics.LEFT);
			}
		}
	}
}

class JustifiedLevelLayer implements Drawable {
	final byte[] bg;
	public int speed;
	int maxShift, justCenterTile;
	
	public JustifiedLevelLayer(byte[] _bg, int _speed) {
		speed = _speed;
		bg = _bg;
	}

	public void postInit() {
		maxShift = (maxBGShift*((maxbgspeed<<8)/speed))>>8;

		justCenterTile = (pwidth+maxShift)/32;
		if (((pwidth+maxShift)&0x1f) != 0) justCenterTile++;
		if (justCenterTile > 6) justCenterTile = 6;
	}
	
	public void draw(Graphics g) {
		// bgpos ujrakalkulalasa
		int bgPos = (int)((accFrameTime*speed)>>16) + (accbgpos>>8);
		
		int bgPosMod = bgPos&0x1f;
		int bgPosDiv = bgPos>>5;
		int vscrleft = (((vscrdelta*speed)+32768)>>16);
		
		// kirajzolas
		// bal oldal
		g.setClip(pleft, ptop, pwidth>>1, pheight);
		for (int i = 1; i <= tileHeightToDraw; i++) {
			int bgIndex = (bgPosDiv+i-1)*12;
			int topPos = i<<5;

			for (int j = 0; j < justCenterTile; j++) {
				int actImIndex = bg[bgIndex++];
				if (actImIndex < 0 || actImIndex >= bgImages.length) continue;

				// Y irany check
				if (topPos-bgHeight[actImIndex] > pheight+bgPosMod) continue;

				int leftPos = (j<<4)+vscrleft;

				g.drawImage(bgImages[actImIndex], pleft+leftPos, pbottom + bgPosMod - topPos, Graphics.TOP|Graphics.LEFT);
			}
		}

		//jobb oldal
		g.setClip(pwidth>>1, ptop, pwidth>>1, pheight);
		for (int i = 1; i <= tileHeightToDraw; i++) {
			int bgIndex = (bgPosDiv+i-1)*12+6;
			int topPos = i<<5;

			for (int j = 6; j < 12; j++) {
				int actImIndex = bg[bgIndex++];
				if (actImIndex < 0 || actImIndex >= bgImages.length) continue;

				// X irany check
				int leftPos = pwidth+vscrleft - ((12-j)<<4);
				if (leftPos >= pright || bgWidth[actImIndex]+leftPos < pleft+(pwidth>>2)) continue;

				// Y irany check
				if (topPos-bgHeight[actImIndex] > pheight+bgPosMod) continue;

				g.drawImage(bgImages[actImIndex], pleft+leftPos, pbottom + bgPosMod - topPos, Graphics.TOP|Graphics.LEFT);
			}
		}
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
