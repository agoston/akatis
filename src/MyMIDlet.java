/* AKATIS by hory (horvath.agoston@gmail.com)
 *
 * Pretty fucked up code, but I had to take several things serious:
 *      - number of .class files - since they are stored in a .zip, each class eats up 100bytes minimum, plus the compressed class file
 *      - garbage collector. The more active objects in memory, the slower/more ineffective it runs.
 *      - function calls. every non-static function call takes a considerable amount of time, so I had to reduce them to bare minimum.
 *      - accesses to object instances or their fields are slower than accesses to static fields in local class
 *
 *
 *  Still, I fucked it up. Performance is good, but having ~4000 lines of foobar to maintain and hack is really hard (if not impossible).
 *  Also, performance of fucked-up code can never be good. Not mentioning the code duplication (mostly happened in level paint()).
 *
 *  I think the best code and design would be to:
 *      - BDUF - Big Design Up Front. Have myself design every little detail (like functions, what they do, how they do it, and what other functions they rely on)
 *      - Don't bother with performance throughout development (except for the high impact parts and the ones where one couldn't optimize easily in the end)
 *      - Write code fully object-oriented, but not that fully; I mean, don't put everything in a new class, make hundreds of interfaces and inheritances, but
 *              do put things which belong in a class to their respective classes to avoid code duplication and fubar.
 *      - Use a GOOD IDE. Eclipse turned out to be rather good in this. A good debugger also helps.
 *      - Use good backup tools & a version control system (two-fold reason: to prove that the code belongs to you and to have backups of older versions if needed)
 *      - Keep the time schedule, or you are GOING TO BE swamped.
 *      - Don't store anything in final static variables, unless you're absolutely sure that you/the designer/etc... won't change that. Create a data file instead, and
 *              store every gameplay-related constants there. Or you gonna suck with lo/hi -res, midp1/2, etc... Actually, that's a basic configuration management.
 *      - Plan on using some configuration management (heck, even a cpp will do fine), otherwise you will suck in the end when you'd need to do slightly different
 *              versions on different mobiles
 *
 *
 *  That's pretty much my experience from writing this code.
 *  I sucked big time with the event handler system; midp runs all important stuff serially, so I'd have to render to an off-screen buffer in a different thread -
 *  but that's another ~30K of precious memory, which is a no-go for low-end, low-fps systems for which the event handler was designed for in the first place. Fuck.
 *
 *  hory, 2005 oct 11, Budapest, Hungary.
 *
 */

import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import javax.microedition.rms.*;
import javax.microedition.media.*;
import java.util.*;
import java.io.*;

/**********************************************************************************
/* Misc. stuff
/**********************************************************************************/
final class Stuff {
	public static final String readLine(InputStream is) throws IOException {
		byte[] buf = new byte[4096];
		int index = 0;
		buf[index] = (byte)is.read();

		for (;;) {
			if (buf[index] < 0 || buf[index] == '\n') break;
			buf[++index] = (byte)is.read();
		}
		return new String(buf, 0, index);
	}

	public static final int ln2(int in) {
		int ret = -1;
		for (; in != 0; ret++) in >>= 1;
		return ret;
	}

	public static final int ln10(int in) {
		if (in < 10) return 0;
		if (in < 100) return 1;
		if (in < 1000) return 2;
		if (in < 10000) return 3;
		if (in < 100000) return 4;
		if (in < 1000000) return 5;
		if (in < 10000000) return 6;
		if (in < 100000000) return 7;
		if (in < 1000000000) return 8;
		return 9;
	}

	public static final int greatestPowerOfTen(int in) {
		if (in < 10) return 0;
		if (in < 100) return 10;
		if (in < 1000) return 100;
		if (in < 10000) return 1000;
		if (in < 100000) return 10000;
		if (in < 1000000) return 100000;
		if (in < 10000000) return 1000000;
		if (in < 100000000) return 10000000;
		if (in < 1000000000) return 100000000;
		return 1000000000;
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

	// 3 feltetel, hogy a teljes ertekkeszleten vegigmenjen (donald knuth, 2 kotetbol):
	// seed = (a * seed + c) mod m
	// 1. c es m relativ primek
	// 2. (a-1) = k*m, k>=1
	// 3. (a-1) mod 4 = 0, ha m mod 4 = 0
	public static int seed = 32767;
	public static final int random16() {	// 16 bites random
		seed = (458759*seed+71) & 65535;
		return seed;
	}

	// sin+cos: tablazattal
	public final static int sin[] = {0, 1143, 2287, 3429, 4571, 5711, 6850, 7986, 9120, 10252, 11380, 12504, 13625, 14742, 15854, 16962, 18064, 19160, 20251, 21336, 22414, 23486,
									24550, 25607, 26655, 27696, 28729, 29752, 30767, 31772, 32768, 33753, 34728, 35693, 36647, 37589, 38521, 39440, 40348, 41243, 42125, 42995,
									43852, 44695, 45525, 46340, 47142, 47930, 48702, 49460, 50203, 50931, 51643, 52339, 53019, 53683, 54331, 54963, 55577, 56175, 56755, 57319,
									57864, 58393, 58903, 59395, 59870, 60326, 60763, 61183, 61583, 61965, 62328, 62672, 62997, 63302, 63589, 63856, 64103, 64331, 64540, 64729,
									64898, 65047, 65177, 65286, 65376, 65446, 65496, 65526, 65536};

	public static final int sin(int degree) {
		if (degree >= 180) {
			if (degree >= 270) {		// 269-359
				return -sin[360-degree];
			} else {					// 180-269
				return -sin[degree-180];
			}
		} else {
			if (degree >= 90) {			// 90-179
				return sin[180-degree];
			} else {					// 0-89
				return sin[degree];
			}
		}
	}

	public static final int cos(int degree) {
		if (degree >= 180) {
			if (degree >= 270) {		// 269-359
				return sin[degree-270];
			} else {					// 180-269
				return -sin[270-degree];
			}
		} else {
			if (degree >= 90) {			// 90-179
				return -sin[degree-90];
			} else {					// 0-89
				return sin[90-degree];
			}
		}
	}
}

/**********************************************************************************
/* MAIN
/**********************************************************************************/
final class MyCanvas extends Canvas implements Runnable {

    // LOWRES BEGIN
//	static final int[] numberWidth = {6, 6, 7, 6, 8, 6, 6, 7, 7, 6};
//	static final int[] fontWidth = {3, 8, 7, 8, 8, 7, 7, 8, 8, 5, 5, 7, 6, 9, 8, 9, 7, 9, 6, 7, 7, 8, 7, 11, 7, 7, 7, 3, 3, 3, 6, 3, 4, 5, 6, 6, 4, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7};
//	static final int[] shopNumberWidth = {8, 7, 7, 6, 6, 7, 7, 7, 7, 7};
//
//    static final int bottomButtonYOffset = 8, bottom1ButtonXOffset = 6, bottom3ButtonXOffset = 0;
//    static final int top1NumXOffset = 6, top1NumYOffset = 1, top3NumXOffset = 20, top3NumYOffset = 1;
//	static final int[] extraGunVerticalTilt = {2<<8, 3<<8, 3<<8};
//    static final int menuPointYOffset = 9;
//    static final int weaponInfo1YOffset = 4, weaponInfo2YOffset = 15, weaponInfo3YOffset = 24;
//    static final int fBoss1WingOffsetX = 30<<8, fBoss1WingOffsetY = 22<<8;
//    static final int fBoss2WingOffsetX = 39<<8, fBoss2WingOffsetY = 24<<8;
//    static final int fBoss3WingOffsetX = 39<<8, fBoss3WingOffsetY = 24<<8;
//    static final int scrDissolveSize = 1;
//    static final int rocketParticleYOffset = 6<<256;
    // LOWRES END

    // HIRES BEGIN
	static final int[] numberWidth = {10, 8, 10, 10, 10, 10, 10, 10, 10, 10};
	static final int[] fontWidth = {5, 9, 9, 10, 11, 9, 8, 10, 10, 5, 7, 8, 8, 11, 10, 11, 9, 11, 9, 9, 10, 10, 9, 15, 9, 9, 9, 3, 4, 3, 8, 3, 4, 6, 7, 7, 4, 9, 7, 8, 8, 9, 8, 9, 8, 9, 9, 0};
	static final int[] shopNumberWidth = {9, 7, 8, 8, 9, 8, 9, 8, 9, 9};

    static final int bottomButtonYOffset = 10, bottom1ButtonXOffset = 8, bottom3ButtonXOffset = 5;
    static final int top1NumXOffset = 8, top1NumYOffset = 2, top3NumXOffset = 27, top3NumYOffset = 2;
	static final int[] extraGunVerticalTilt = {3<<8, 4<<8, 4<<8};
    static final int menuPointYOffset = 14;
    static final int weaponInfo1YOffset = 6, weaponInfo2YOffset = 19, weaponInfo3YOffset = 32;
    static final int fBoss1WingOffsetX = 39<<8, fBoss1WingOffsetY = 24<<8;
    static final int fBoss2WingOffsetX = 39<<8, fBoss2WingOffsetY = 24<<8;
    static final int fBoss3WingOffsetX = 39<<8, fBoss3WingOffsetY = 24<<8;
    static final int scrDissolveSize = 2;
    static final int rocketParticleYOffset = 8<<256;
    // HIRES END

    // =================================================> FIXME: LORES-bol kiszedni a particleImage-t!

	static final int STATE_START = 0;
	static final int STATE_MENU_IN = 1;
	static final int STATE_MENU_DOOR_OUT = 7;
	static final int STATE_MENU = 2;
	static final int STATE_MENU_NEWQUESTION = 36;
	static final int STATE_MENU_UPGRADE_OPEN = 9;
	static final int STATE_MENU_UPGRADE = 11;
	static final int STATE_MENU_UPGRADE_CLOSE = 10;
	static final int STATE_MENU_DOOR_IN = 8;
	static final int STATE_MENU_TO_GAME = 3;
	static final int STATE_MENU_CHAPTER_OPEN = 33;
	static final int STATE_MENU_CHAPTER = 34;
	static final int STATE_MENU_CHAPTER_CLOSE = 35;
	static final int STATE_GAME = 4;
	static final int STATE_GAME_TOP_IN = 12;
	static final int STATE_GAME_TOP_OUT = 13;
	static final int STATE_GAME_TO_MENU_PAR = 5;
	static final int STATE_GAME_QUITQUESTION = 32;
	static final int STATE_MENU_OUT = 6;
	static final int STATE_WEAPONINFO_IN = 14;
	static final int STATE_WEAPONINFO_OUT = 15;
	static final int STATE_NOT_ENOUGH_MONEY = 16;
	static final int STATE_DISSOLVE_PAR = 17;
    static final int STATE_MENU_TO_SCORES = 18;
    static final int STATE_SCORES = 19;
    static final int STATE_SCORES_TO_MENU = 20;
    static final int STATE_MENU_TO_STORY = 21;
    static final int STATE_STORY = 22;
    static final int STATE_STORY_NEXT_PAGE = 23;
    static final int STATE_STORY_TO_GAME = 24;
    static final int STATE_GAME_TO_STORY = 25;
    static final int STATE_STORY_TO_MENU = 26;
    static final int STATE_STORY_TO_SCORES = 30;
    static final int STATE_GAME_LEVEL_END = 27;
    static final int STATE_EXIT = 28;
    static final int STATE_GAME_TO_SCORES = 29;
	static final int STATE_LOGO = 31;
	public static int state = STATE_LOGO, nextState, prevState;

    static final int FLASH_RED = 1, FLASH_BLUE = 2, FLASH_WHITE = 3;

    public static Display myDisplay;
	public static MyMIDlet myMIDlet;
	static final int shipSpeed = 64, extraGunSpeed = 48;
	static final int maxBGShift = 16;
	static final int maxShipSlideTime = 192;

	static int scrX, scrY, realscrX, realscrY, pleft, pright, ptop, ptop2, pbottom;
	static int fShipX, fShipY, fShipSlidePos, fShipLimitX, fShipLimitY, shipHP, shipPrevHP, maxShipHP = 100, shipSlideTime, shipDestroyTime, shipFrostTime;
	static int shipWeaponFront=-1, shipWeaponBack=-1, shipWeaponShield = -1, shipWeaponExtra = -1;
	static int fExtraGunX, fExtraGunY;
	static int shipFrontFired, shipBackFired, shipExtraFired;
	static boolean shipWeaponFrontDualLeft, shipWeaponBackDualLeft, shipWeaponExtraLeft = false;
	static int pwidth, pheight, pmiddle, penwidth, fpenwidth, penmiddle;
	static int vscrl, vscrr, vscrdelta, maxvscrdelta, fMaxBGSpeed, fEnemyBGSpeed, fMaxEnShift, fenshift, enshift, bgshift, enbgpos;
	static int tileHeightToDraw;

	static long levelStartTime;
	static int lastFrameTime, accFrameTime, lastFrameStart, actFrameStart;
    static int fAccBGTime;

	static Image[] bgImages;
	static int[] bgWidth, bgHeight;
	static Drawable[] bgLayers;

	static Enemy[] enemies;
	static Image[] enemyImages = new Image[32];
	static final int[] enWidth = new int[32];
	static final int[] enHeight = new int[32];
	static final int[] fenWidth = new int[32];
	static final int[] fenHeight = new int[32];
    static final int[] enExpl = {2,2,0,0, 0,0,0,2, 0,0,0,0, 0,0,0,0, 0,0,0,0};
    static int explSize, fExplSize;
	static int enBegin = 0;

	static final Image[] bulletImages = new Image[38];
	// TODO: bulletmereteket static final-ba rakni (speedup/size)
	static final int[] bulletHeight = new int[38];
	static final int[] bulletWidth = new int[38];
	static final int[] fBulletHeight = new int[38];
	static final int[] fBulletWidth = new int[38];
	public static final Bullet[] bullets = new Bullet[64];
	public static int bulletLast = 0;
	static final int[] weaponRate = {95, 80, 128, 95, 100, 220, 230, 150, 150, 160, 160, 160, 256, 190, 190, 460};
	static final int[] weaponPrice = {100, 200, 300, 200, 400, 600, 500, 1000, 1500, 3000, 2000, 1000, 500, 1000, 2500, 5000,
										1500, 3000, 1500, 3000, 4500};	//shield, extragun
	static final int[] weaponPower = {1, 2, 3, 2, 3, 4, 3, 4, 5, 5, 6, 7, 2, 4, 4, 5};
	static final int[] bulletFrameNum =	{	1, 3, 2, 2, 3, 3, 3, 3, 3, 3,
											3, 1, 1, 1, 1, 1, 1, 1, 3, 3,
											3, 3, 3, 3, 3, 3, 3, 3, 3, 1,
											1, 1, 1, 1, 1, 1, 3, 3, 1, 1};

	static Image[] explosionImages;

	static Image shipImage;
	static int shipWidth, shipHeight, fShipWidth, fShipHeight;

	static Path[] pathes;

    // font
    static final char[] fontChar = {' ', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
                                    '.', ',', '!', '?', ':', ';', '-', '/', '"', '\'', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '\\'};
	static final int[] fontStart = new int[fontChar.length];
	static final int[] numberStart = new int[10];
	static final int[] shopNumberStart = new int[10];
    static int fontHeight, numberHeight, shopNumberHeight;
	static Image shopNumberImage;
    static Image numberImage;
	static Image fontImage;

    // top
	static Image topBorder, topLives, topScore;
	static int topScorePos, topLivesPos, topBorderPos;

	// bottom
	static Image bottomLeftDisplay, bottomRightDisplay, bottomBorder;
	static int bottomLeftDisplayPos, bottomRightDisplayPos, bottomBorderPos;
	static Image bottomNext, bottomPrev, bottomOK, bottomHP;
	static int bottomLeftIconPos, bottomRightIconPos;

	// sizes
	static int topHeight, top1Width, top3Width, top1Height;
    static int bottomHeight, bottom1Width, bottom3Width, button1Width, button3Width, buttonHeight;
    static int leftDoorWidth, rightDoorWidth;
    static int weaponInfoHeight, actMenuHeight;

	// menu
	static int accMenuTime;
	static Image menuBG, menuBGAct, doorImage;
	static int leftDoorPos, rightDoorPos, weaponInfoPos;
	static Image upgradeBG, weaponInfoImage;

	static Image[] chapterBigImages;
	static Image[] chapterLabelImages;
	static Image[] chapterNumImages;
	static int chapterBigImageX;
	static int chapterScrollDelta;
	static int chapterBigImageIndices[] = { 0, 0, 0, 1, 1, 1, 2, 2, 2, 1 };
	static int selectedChapter;

	static int menuParent;
	static int menuSelect, menuOffset;
	static int[] menu =     {	 0, 1,  2,  3,  4,		// 0	game, settings, control, about, exit
								 5, 6,  7,  8, -1,		// 5	new, continue, scores, back
								 9, 11, 8, -1, -1,		// 10	start, upgrade, tutorial, back
								13, 14, 15, 16, 8,		// 15	UNUSED //uct l, uct h, rtn l, rtn h
								17, 18, 19, 20, 8,		// 20	front, rear, shield, extra, back
								21, 22, 23, 24, 8,		// 25	next, prev, buy, sell, back
								25, 26,  14, 8,-1,		// 30	sound on, sound off, normal, back
								13, 14, 15, 8, -1,		// 35	easy, normal, hard, back		
                                -1};

	static final int[] menuJump = {	5,	30,	-1,	-1,	-1,
									10,	10,	-1,	 0,	-1,
									-1,	20,	5, -1,	-1,
									-1,	-1,	-1,	-1,	10,     // UNUSED
									25,	25,	25,	25,	10,
									-1,	-1,	-1,	-1,	20,
									-1, 	-1,	35,	0, 	-1,
									-1,	-1,	-1,	30	-1};

	static String[] messages;
	static EnemyType[] entypes = null;
	static int messageID, messageTime = -1;
	static final int weaponNameIndex = 30;
	static final int gameMessageIndex = 39;
	static final int storyMessageIndex = 43;

	static int shipBlinkingTime = -1;
	static Random random = new Random();

    static int shipFlashTime = -1, shipFlashType = -1, shipFlashWidth, shipFlashHeight;

// HI-RES BEGIN
    static Image particleImages[];
// HI-RES END
    static int[] particlefXPos, particlefYPos;
	static int[] particlefTime;
	static int[] particleAngleSin, particleAngleCos;
    static int[] particleSpeed, particleAnimPos;

	static int score = 0, actScore = 0, levelNum = 0, maxLevelNum = 0, levelNumHPScore = 0;
    static int shopWeaponFront, shopWeaponBack, shopWeaponShield, shopWeaponExtra;

	static final int[] shieldBulletIndex = new int[2];

    static final String[] highScoreNames = new String[8];
    static final int[] highScores = new int[8];

    static int[] storyPageIndex = new int[32];
    static int actStory = storyMessageIndex, actStoryPage = 0, prevStoryPage = 0, lastStoryPage = Integer.MAX_VALUE;   //FIXME: ezeket reinit story-ba belepeskor
    static int storyMargin, storyWidth;

    static int prevMainEnemyHP = -1;
    static Enemy mainEnemy = null;
    static boolean scrollLevel = true;
    static int[] meDestroyTime = null, meDestroyX = null, meDestroyY = null;
    static boolean mainEnemyDualLeft = false;
    static int mainEnemyFront = 0;

    static int playerLives = 2;
    static final int[] storyBeforeLevel = {0, -1, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
    static final int[] storyAfterLevel = {-1, -1, -1, -1, 2, -1, -1, 3, -1, 4, -1, -1, 5, -1, 6};

    static boolean highScorePossible = false;

    static boolean shouldDrawShip = true;

	static int prevPlayerLives, prevScore;

    public static int bulletSpeed;

    static Image shipEngineParticleImage;
    static int shipEngineParticleXOffset, shipEngineParticleYOffset, shipEngineParticleXSize, shipEngineParticleYSize;
    static int shipEngineParticleSpawnTime;

    public static Player player;
    public static String actMusic;
    public static boolean musicOn = true;

//    static Player[] soundPlayer;
    static final int SOUND_EXPLOSION = 0, SOUND_SHOT = 1, SOUND_UPGRADE = 2;
    static final String[] soundFiles = {"expl.wav", "shot.wav", "upg.wav"};

    static Class myClass;
    static MyCanvas myThis;

	// TODO: konstruktorbol inicializalni egy halom objektumot, amik folyamatosan a memoriaban lesznek
	public MyCanvas() {
        // MIDP2
        setFullScreenMode(true);

        scrX = 176;
		scrY = 208;

        realscrX = getWidth();
		realscrY = getHeight();

		pleft = 0;
		pright = scrX;

		pwidth = pright-pleft;
		pmiddle = (pleft+pright)/2;

		vscrl = (pwidth>>1) - 96;
		vscrr = (pwidth>>1) + 96;

		shieldBulletIndex[0] = shieldBulletIndex[1] = -1;
        storyMargin = scrX>>5;
        storyWidth = scrX - (2*storyMargin);

        shipFlashWidth = scrX>>5;
        bulletSpeed = (scrY<<8)/128;

        myClass = getClass();
        myThis = this;
	}

/**********************************************************************************
/* LOAD
/**********************************************************************************/
	public static final void loadStatic() {
		// load all-time-needed images
		try {
			// intl messages
			Vector v = new Vector(20,10);
			InputStream is = myClass.getResourceAsStream("/intl");

			for (int i = 0; ; i++) {
				String imageFile = Stuff.readLine(is);
				if (imageFile.length() == 0) break;
				if (imageFile.charAt(0) == '#') continue;
				v.addElement(imageFile);
			}
			messages = new String[v.size()];
			v.copyInto(messages);

			// ship images
			shipImage = Image.createImage("/pl.png");
            shipWidth = shipImage.getWidth()/7;
            shipHeight = shipImage.getHeight();
            fShipWidth = shipWidth<<8;
            fShipHeight = shipHeight<<8;

			// bullets
			for (int i = 0; i < bulletImages.length; i++) {
				bulletImages[i] = Image.createImage("/en/b"+(i/10)+(i%10)+".png");
				bulletWidth[i] = bulletImages[i].getWidth()/bulletFrameNum[i];
				bulletHeight[i] = bulletImages[i].getHeight();
				fBulletWidth[i] = bulletWidth[i]<<8;
				fBulletHeight[i] = bulletHeight[i]<<8;
			}

			// number & font images
			fontImage = Image.createImage("/font.png");
			numberImage = Image.createImage("/num.png");

            int actFontStart = 0;
            for (int i = 0; i < fontChar.length; i++) {
                fontStart[i] = actFontStart;
                actFontStart += fontWidth[i];
            }
            actFontStart = 0;
            for (int i = 0; i < 10; i++) {
                numberStart[i] = actFontStart;
                actFontStart += numberWidth[i];
            }
            fontHeight = fontImage.getHeight();
            numberHeight = numberImage.getHeight();

            // bottom images
			bottomLeftDisplay = Image.createImage("/bottom_1.png");
			bottomBorder = Image.createImage("/bottom_2.png");
			bottomRightDisplay = Image.createImage("/bottom_3.png");

			// top images
			topBorder = Image.createImage("/top.png");
			topLives = Image.createImage("/top_live.png");
			topScore = Image.createImage("/top_score.png");

            topHeight = topBorder.getHeight();
            top1Width = topScore.getWidth();
            top1Height = topScore.getHeight();
            top3Width = topLives.getWidth();

            bottomHeight = bottomBorder.getHeight();
            bottom1Width = bottomLeftDisplay.getWidth();
            bottom3Width = bottomRightDisplay.getWidth();

            shipFlashHeight = scrY - bottomHeight - Math.max(top1Height, topLives.getHeight());

            // high score
            loadHighScore();

            // init screen size constants
            ptop = topHeight;
            ptop2 = Math.max(topHeight, top1Height);
            pbottom = scrY - bottomHeight;
            pheight = pbottom-ptop;
            tileHeightToDraw = ((pheight+128)>>5);
            if ((pheight & 0x1f) > 0) tileHeightToDraw++;

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    public void setLayer(int i, int stretch, byte[] bg, int speed) {
        switch (stretch) {
            case 0:
                bgLayers[i] = new CenteredLevelLayer(bg, speed); break;
            case 1:
                bgLayers[i] = new RepeatedLevelLayer(bg, speed); break;
            case 2:
                bgLayers[i] = new JustifiedLevelLayer(bg, speed); break;
        }
    }

	public static final void loadLevel(int levelNum) {
		try {
			// enemy types
			if (entypes == null) entypes = new EnemyType[64];
			InputStream is = myClass.getResourceAsStream("/entypes");
			String actLine = Stuff.readLine(is);
			for (int i = 0; actLine != null && actLine.length() > 0; actLine = Stuff.readLine(is), i++) {
				if (actLine.startsWith("#")) {
					i--;
					continue;
				}
				String[] sp = new String[6];
				int actIndex = 0, spInd = 0;
				StringBuffer acts = new StringBuffer();

				while (spInd < 6) {
					char actc = actIndex < actLine.length() ? actLine.charAt(actIndex) : ',';
					if (actc == ',') {
						sp[spInd++] = acts.toString();
						acts.setLength(0);
					} else {
						acts.append(actc);
					}
					actIndex++;
				}
				int[] isp = new int[sp.length];
				for (int j = 0; j < sp.length; j++) isp[j] = Integer.parseInt(sp[j].trim());

				entypes[i] = new EnemyType(isp[0], isp[1], isp[2], isp[3], isp[4], isp[5]);
			}

			// images
			Vector v = new Vector(20,10);
			is = myClass.getResourceAsStream("/l"+levelNum+"trl");

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
			if (pathes == null) {
				is = myClass.getResourceAsStream("/p");
				int pNum = is.read();
				pathes = new Path[pNum];
				for (int i = 0; i < pNum; i++) {
					int len = is.read();
					Path actpath = new Path(len);
					pathes[i] = actpath;
					for (int j = 0; j < len; j++) {
						actpath.x[j] = is.read();
						actpath.y[j] = is.read();
						actpath.speed[j] = is.read();
						actpath.speed[j] += is.read()<<8;
						actpath.wait[j] = is.read();
						actpath.wait[j] += is.read()<<8;
					}
					actpath.init();
				}
			}

			// level layers
			is = myClass.getResourceAsStream("/l"+levelNum);
			int bgNum = is.read();
			bgLayers = new Drawable[bgNum];
			for (int i = 0; i < bgNum; i++) {
				int len = is.read();
				if (len == -1) break;
				len += is.read()<<8;

				int speed = is.read();
				speed += is.read()<<8;
				if (speed > fMaxBGSpeed) fMaxBGSpeed = speed;
				fEnemyBGSpeed = speed;

				int stretch = is.read();

				byte[] bg = new byte[len];
				is.read(bg);

                myThis.setLayer(i, stretch, bg, speed);
			}
			maxvscrdelta = (maxBGShift<<16)/fMaxBGSpeed;
			fMaxEnShift = maxBGShift*((fEnemyBGSpeed<<8)/fMaxBGSpeed);
			penwidth = pwidth+((fMaxEnShift)>>7);
            fpenwidth = penwidth<<8;
			penmiddle = penwidth>>1;
            bgshift = ((maxvscrdelta*fEnemyBGSpeed)+32768)>>16;
            //System.out.println("maxvscrdelta: "+maxvscrdelta+", fenemybgspeed: "+fEnemyBGSpeed+", bgshift: "+bgshift+", pwidth: "+pwidth+", fMaxEnShift: "+fMaxEnShift);

			// propagaljuk - eleg szerencsetlen, de meg mindig a legkisebb eroforrasugenyu
			for (int i = 0; i < bgLayers.length; i++)
				if (bgLayers[i] instanceof JustifiedLevelLayer)
					((JustifiedLevelLayer)bgLayers[i]).postInit();

            // top layer beallitas - pixelre pontos szinkron kell az ellensegekhez
            bgLayers[bgLayers.length-1].setTopLayer();

			// enemies
			int enNum = is.read();
            int villamNum = 0;
			enemies = new Enemy[enNum];
			for (int i = 0; i < enNum; i++) {
				int enTypeNum = is.read();
				int bgPos = is.read();
				bgPos += is.read()<<8;

				if (enTypeNum < 64) {
					EnemyType entype = entypes[enTypeNum];
					int type = entype.imageNum;
					int path = is.read();
					int dx = is.read();
					int dy = is.read();

					enemies[i] = new Enemy(type, bgPos, path, dx, dy, entype);
					if (type == 19) {
                        enemies[i].fwait = villamNum;
                        villamNum++;
                        if (villamNum >= 3) villamNum -= 3;
                    }

					// images
					if (enemyImages[type] == null) {
						enemyImages[type] = Image.createImage("/en/e"+(type/10)+(type%10)+".png");
                        enHeight[type] = enemyImages[type].getHeight();
                        enWidth[type] = enemyImages[type].getWidth();

                        if (type == 9 || type == 10) {  // fast & ghost
                            enWidth[type] /= 5;

                        } else if (type == 19) {    // villam
                            enWidth[type] = scrX+(2*maxBGShift);    // ateri a teljes kepernyot
                            enHeight[type] = 24;
                            enemyImages[18] = Image.createImage("/en/e18.png"); // villam-torony betoltese

                        } else if (type < 20) enWidth[type] >>= 1;  // normal ellen

                        fenHeight[type] = enHeight[type]<<8;
                        fenWidth[type] = enWidth[type]<<8;
					}
				} else {
					switch (enTypeNum) {
						case 64:	{
							byte[] t = new byte[2];
							t[0] = (byte)is.read();
							t[1] = (byte)is.read();
							enemies[i] = new Enemy(enTypeNum, bgPos, t);
							break;
						}

						case 66: {
							byte[] t = new byte[bgNum*2];
							for (int l = 0; l < t.length; l++) t[l] = (byte)is.read();
							enemies[i] = new Enemy(enTypeNum, bgPos, t);
							break;
						}

						default:
							enemies[i] = new Enemy(enTypeNum, bgPos, null);
					}
				}
			}

			// explosion images
			if (explosionImages == null) {
				explosionImages = new Image[3];
				for (int i = 0; i < 3; i++) {
					explosionImages[i] = Image.createImage("/ex"+i+".png");
				}
                explSize = explosionImages[0].getHeight();
                fExplSize = explSize<<8;
			}

// HI-RES BEGIN
            if (particleImages == null) {
                particleImages = new Image[6];
                for (int i = 0; i < 6; i++) {
                    particleImages[i] = Image.createImage("/part"+i+".png");
                }

                // engine particle
                shipEngineParticleImage = Image.createImage("/ps.png");
                shipEngineParticleXSize = shipEngineParticleImage.getWidth()>>1;
                shipEngineParticleYSize = shipEngineParticleImage.getHeight();
                shipEngineParticleXOffset = (shipWidth>>1)-(shipEngineParticleXSize>>1);
                shipEngineParticleYOffset = shipHeight-1-(shipHeight/24);
            }
// HI-RES END

			if (bottomHP == null) {
				bottomHP = Image.createImage("/bottom_3_hp.png");
			}

			// particle
			particlefXPos = new int[64];
			particlefYPos = new int[64];
			particlefTime = new int[64];
			particleSpeed = new int[64];
			particleAngleSin = new int[64];
			particleAngleCos = new int[64];
			particleAnimPos = new int[64];

			// sound
//            if (soundPlayer == null) {
//                soundPlayer = new Player[soundFiles.length];
//                for (int i = 0; i < soundFiles.length; i++) {
//                    try {
//                        // FIXME
//                        //soundPlayer[i] = Manager.createPlayer(getClass().getResourceAsStream(soundFiles[i]), "audio/x-wav");
//                        //soundPlayer[i].prefetch();
//                    } catch (Exception e) {}
//                }
//            }

		} catch (Exception e) {	// TODO: delete this
			e.printStackTrace();
		}
	}

	public static final void disposeLevel() {
		bgLayers = null;
		bgImages = null;
		bgWidth = null;
		bgHeight = null;
		enemies = null;
		enemyImages = new Image[32];
		pathes = null;
		explosionImages = null;
		bottomHP = null;
		entypes = null;
		particlefXPos = particlefYPos = particlefTime = particleAngleSin = particleAngleCos = particleSpeed = particleAnimPos = null;
        particleImages = null;
        shipEngineParticleImage = null;

        //if (soundPlayer != null) {
            //for (int i = 0; i < soundPlayer.length; i++) soundPlayer[i].close();
            //soundPlayer = null;
        //}
	}

	public static final void loadMenu(boolean full) {
		try {
			// menu images
			if (full && menuBG == null) {
                menuBG = Image.createImage("/m.png");
                menuBGAct = Image.createImage("/mact.png");
                rightDoorWidth = menuBG.getWidth();
                actMenuHeight = menuBGAct.getHeight();
            }

            if (doorImage == null) {
                doorImage = Image.createImage("/door.png");
                leftDoorWidth = doorImage.getWidth();
            }

            // bottom menuimages
            if (bottomNext == null) {
                bottomNext = Image.createImage("/bottom_3_next.png");
                bottomPrev = Image.createImage("/bottom_1_back.png");
                bottomOK = Image.createImage("/bottom_3_ok.png");   // FIXME: ezt egyelore sehol sem hasznalom, ki kelleve venni

                button1Width = bottomPrev.getWidth();
                button3Width = bottomNext.getWidth();
                buttonHeight = bottomNext.getHeight();
            }

			// upgrade Images
			if (full && upgradeBG == null) {
				upgradeBG = Image.createImage("/door_space.png");
				weaponInfoImage = Image.createImage("/winf.png");
				shopNumberImage = Image.createImage("/wfont.png");

                int actFontStart = 0;
                for (int i = 0; i < 10; i++) {
                    shopNumberStart[i] = actFontStart;
                    actFontStart += shopNumberWidth[i];
                }
                shopNumberHeight = shopNumberImage.getHeight();

                weaponInfoHeight = weaponInfoImage.getHeight();
			}
			if (chapterBigImages==null) {
				chapterBigImages = new Image[3];
				chapterBigImages[0] = Image.createImage("/c1.png");
				chapterBigImages[1] = Image.createImage("/c2.png");
				chapterBigImages[2] = Image.createImage("/c3.png");
				chapterLabelImages = new Image[2];
				chapterLabelImages[0] = Image.createImage("/chapter1.png");
				chapterLabelImages[1] = Image.createImage("/chapter2.png");
				chapterNumImages = new Image[2];
				chapterNumImages[0] = Image.createImage("/c1font.png");
				chapterNumImages[1] = Image.createImage("/c2font.png");
			}

		} catch (Exception e) {	// TODO: delete this
			e.printStackTrace();
		}
	}

	public static void disposeMenu(boolean full) {
		menuBG = menuBGAct = null;
        if (full) {
            doorImage = bottomNext = bottomPrev = bottomOK = null;
        }
		upgradeBG = weaponInfoImage = shopNumberImage = null;
	}

/**********************************************************************************
/* MOVE
/**********************************************************************************/
	public void init() {
        // ora start
        levelStartTime = System.currentTimeMillis();
		actFrameStart = 0;
        accMenuTime = 0;

		//initLevel(0);
		//state = STATE_GAME;

        // enter main cycle
        //new Thread(this).start();
		run();
	}

    final static void saveGame() {
        try {
            deleteGame();

            RecordStore actrs = RecordStore.openRecordStore("game", true);

            byte[] data = (fAccBGTime > 0) ? new byte[15] : new byte[8];

            data[0] = (byte)maxLevelNum;
            data[1] = (byte)((score>>8) & 0xff);
            data[2] = (byte)(score & 0xff);
            data[3] = (byte)(shipWeaponFront);
            data[4] = (byte)(shipWeaponBack);
            data[5] = (byte)(shipWeaponShield);
            data[6] = (byte)(shipWeaponExtra);
            data[7] = (byte)(playerLives);

            if (fAccBGTime > 0) {
                //System.out.println("faccbgtime: "+fAccBGTime);
                data[8] = (byte)((fAccBGTime>>24)&0xff);
                data[9] = (byte)((fAccBGTime>>16)&0xff);
                data[10] = (byte)((fAccBGTime>>8)&0xff);
                data[11] = (byte)(fAccBGTime&0xff);
                data[12] = (byte)(shipHP);
                data[13] = (byte)((actScore>>8)&0xff);
                data[14] = (byte)(actScore&0xff);
            }

            int recId = actrs.addRecord(data, 0, data.length);
            actrs.closeRecordStore();
            //System.out.println("SaveGame: "+recId);

        } catch (RecordStoreException e) {
            e.printStackTrace();
        }
    }

    final static void deleteGame() {
        try {
            RecordStore.deleteRecordStore("game");
        } catch (RecordStoreException e) {}
    }

    final static boolean loadGamePossible() {
        try {
            RecordStore actrs = RecordStore.openRecordStore("game", false);
            actrs.closeRecordStore();
        } catch (RecordStoreException e) {
            return false;
        }
        return true;
    }

    final static void loadGame() {
        //System.out.print("Loading game:");
        byte[] data = null;
        try {
            RecordStore actrs = RecordStore.openRecordStore("game", false);
            //System.out.print(" Size: "+actrs.getRecordSize(1));
            data = actrs.getRecord(1);
            actrs.closeRecordStore();
        } catch (RecordStoreException e) {}

        if (data != null) {
            //System.out.print(" reset+data");
            resetGame();
            maxLevelNum = data[0];
            score = (data[1]<<8)+data[2];
            shipWeaponFront = data[3];
            shipWeaponBack = data[4];
            shipWeaponShield = data[5];
            shipWeaponExtra = data[6];
            playerLives = data[7];

            if (data.length > 8) {
                fAccBGTime = (data[8]<<24)+(data[9]<<16)+(data[10]<<8)+data[11];
                //System.out.print(" faccbgtime: "+fAccBGTime);
                shipHP = data[12];
                actScore = (data[13]<<8)+data[14];
            }
        }
        //System.out.println(" done.");
        //System.out.flush();
    }

    final static int asubyte(byte v) {
        if (v<0) return v+256;
	return v;
    }

    final static void loadHighScore() {
        byte[] data = null;
        try {
            RecordStore actrs = RecordStore.openRecordStore("hs", false);
            data = actrs.getRecord(1);
            actrs.closeRecordStore();
        } catch (RecordStoreException e) {}

        if (data != null) {
		//System.out.println("score load");
            for (int i = 0; i < data.length/10; i++) {
                highScoreNames[i] = (new String(data, i*10, 8)).replace((char)0, ' ');
                highScores[i] = (asubyte(data[i*10+8])<<8)+asubyte(data[i*10+9]);
		//System.out.println(" "+data[i*10+8]+" "+data[i*10+9]+" "+highScores[i]);
            }
        } else {
            highScoreNames[0] = "ghost   ";
            highScores[0] = 10000;
            highScoreNames[1] = "barrage ";
            highScores[1] = 8000;
            highScoreNames[2] = "sam     ";
            highScores[2] = 6000;
            highScoreNames[3] = "wildcat ";
            highScores[3] = 5000;
            highScoreNames[4] = "bear    ";
            highScores[4] = 4000;
            highScoreNames[5] = "unknown ";
            highScores[5] = 3000;
            highScoreNames[6] = "unknown ";
            highScores[6] = 2000;
            highScoreNames[7] = "unknown ";
            highScores[7] = 1000;
        }
    }

    final static void saveHighScore() {
        try {
            try {
                RecordStore.deleteRecordStore("hs");
            } catch (RecordStoreException e) {}

            RecordStore actrs = RecordStore.openRecordStore("hs", true);

            int recnum = 0;
            while (recnum < 8 && highScoreNames[recnum] != null) recnum++;

            byte[] data = new byte[recnum*10];
            for (int i = 0; i < recnum; i++) {
                byte[] actname = highScoreNames[i].getBytes();
                System.arraycopy(actname, 0, data, i*10, actname.length);
                data[i*10+8] = (byte)((highScores[i]>>8)&0xff);
                data[i*10+9] = (byte)((highScores[i])&0xff);
            }

            actrs.addRecord(data, 0, data.length);
            actrs.closeRecordStore();

        } catch (RecordStoreException e) {}
    }

    final static void resetGame() {
        //System.out.println("RESET Game");
        resetLevel();
        levelNum = maxLevelNum = 0;
        shipWeaponFront = 0;
        shipWeaponBack = 1;
        shipWeaponShield = -1;
        shipWeaponExtra = -1;
        playerLives = 2;
        score = 5000;           // FIXME!!!
    }

    final static void resetLevel() {
        //System.out.println("RESET Level");
        for (int i = 0; i < bullets.length; i++) bullets[i] = null;
        bulletLast = 0;
	fAccBGTime = 0;
	if (menu[32] == 13) maxShipHP = 300;
	else if (menu[32] == 14) maxShipHP = 100;
	else if (menu[32] == 15) maxShipHP = 50;
	shipHP = maxShipHP;
        actScore = 0;
        shouldDrawShip = true;
        prevPlayerLives = prevScore = -1;
        ehLast = ehFirst;   // eh reset
    }

    final static void initLevel() {
        playMidi("/ingame.mid");
        levelNumHPScore = 200;
		loadLevel(levelNum);

        int actbgpos = (fAccBGTime*fEnemyBGSpeed)>>16;
		for (enBegin = 0; enBegin < enemies.length && actbgpos > enemies[enBegin].bgPos; enBegin++);

		accFrameTime = 0;
		// reinit level-dependent values
		fShipLimitX = (penwidth-shipWidth)<<8;
		fShipLimitY = (pheight-shipHeight)<<8;
		fShipX = fShipLimitX>>1;
		fShipY = fShipLimitY;

        //System.out.println("penwidth: "+penwidth+", ship: "+shipWidth+", "+shipHeight);

		if (shipWeaponExtra >= 0) {
			fExtraGunX = fShipX+fShipWidth;
			fExtraGunY = fShipY+fShipHeight-fBulletHeight[15+shipWeaponExtra]-extraGunVerticalTilt[shipWeaponExtra];
		}
        shipPrevHP = -1;    // egyszer rakja ki
        mainEnemy = null;
        meDestroyTime = meDestroyX = meDestroyY = null;
        prevMainEnemyHP = -1;
        scrollLevel = true;
		shipFrontFired = shipBackFired = 0;
		vscrdelta = 0;
		shipSlideTime = maxShipSlideTime/2;
        shipDestroyTime = -1;
        shipFrostTime = -1;
		for (int i = 0; i < bullets.length; i++) bullets[i] = null;
		initShield(shipWeaponShield);
		levelStartTime = System.currentTimeMillis();
		actFrameStart = 0;
		messageID = 1+gameMessageIndex;
		messageTime = shipBlinkingTime = 2*256;
		movePlayer(1, 1, 0);	// init shift values
	}

	static final void initShield(int num) {
		//System.out.println("initshield "+num);
		for (int i = 0; i <= num; i++) {
			if (shieldBulletIndex[i] < 0 || bullets[shieldBulletIndex[i]] == null) {
				int index = 0;
				while (bullets[index] != null) index++;
				shieldBulletIndex[i] = index;

				int angle = i*180;
				if (i != 0) {
					angle = bullets[shieldBulletIndex[i-1]].angle+180;
					if (angle >= 360) angle -= 360;
				}
				bullets[index] = new Bullet(false, -10000, -10000, angle, 0, 14, 100);

				index++;
				if (bulletLast < index) bulletLast = index;
			}
		}

		num++;

		for (int i = num; i < 2; i++) {
			if (shieldBulletIndex[i] >= 0) {
				bullets[shieldBulletIndex[i]] = null;
				shieldBulletIndex[i] = -1;
			}
		}
		//System.out.println(" > "+shieldBulletIndex[0]+", "+shieldBulletIndex[1]);
	}

    static final void initStory(int story) {
        actStory = story; actStoryPage = 0; prevStoryPage = 0; lastStoryPage = Integer.MAX_VALUE;
    }

	public void run() {
        // System.out.println(" >>> "+state);
		// idoszamitas
		if (state!=STATE_GAME_QUITQUESTION) { // hack
			lastFrameStart = actFrameStart;
			actFrameStart = (int)(System.currentTimeMillis()-levelStartTime);
			lastFrameTime = (actFrameStart - lastFrameStart + 2)>>2;
        //if (state == STATE_GAME || state == STATE_GAME_TOP_IN) lastFrameTime >>= 2;
			if (lastFrameTime > 32) lastFrameTime = 32;		// 8 fps alatt lassul a jatek
		//try {Thread.sleep(200);} catch (Exception e) {}
		//lastFrameTime = 64;
			if (scrollLevel) accFrameTime += lastFrameTime;
		}

		// **********************************************************************************************************
		if (state == STATE_GAME || state == STATE_GAME_TOP_IN || state == STATE_MENU_TO_GAME) {
			int unslideTime = lastFrameStart;

			// input eventek lekezelese
			// TODO: synch nem kell, ha serially() fut
			// TODO: ha alacsony fps-nel rossz/lassu az event handler, annak az az oka, hogy a canvas event handler thread-je sorosan hajtja vegre az eventeket - a paint(), key*(), es a run() -t is! Kulon thread kell, az talan javit rajta valamennyit
			//synchronized(ehie) {
				int num = ehSize();
				for (int i = 0; i < num; i++) {
					InputEvent ie = ehNext();
					int length = ie.length;

					// nem iranyitott ido kezelese
					if (unslideTime < actFrameStart) {
						int t = (ie.start-unslideTime)>>2;
						if (t > 0) 	unslidePlayer(t);

						if (length < 0) {
							unslideTime = actFrameStart;
						} else {
							unslideTime = ie.start+ie.length;
						}
					}

					if (length < 0) {	// kiszamoljuk, visszarakjuk
						length = (actFrameStart-ie.start);
						ie.start = actFrameStart;
						ehAdd(ie);
					}

					length >>= 2;	// millisec -> s/256, kozelitoleg, de le van xarva

					if (shipFrostTime < 0 && shipDestroyTime < 0) {
                        switch (ie.key) {
                            case Canvas.UP:
                                movePlayer(0, -1, length); break;

                            case Canvas.DOWN:
                                movePlayer(0, 1, length); break;

                            case Canvas.LEFT:
                                movePlayer(-1, 0, length); break;

                            case Canvas.RIGHT:
                                movePlayer(1, 0, length); break;

                            case Canvas.FIRE:
                                if (!ie.repeated) shipWeaponExtraLeft = !shipWeaponExtraLeft;
                                break;
                        }
                    }

					ie.repeated = true;
				}
			//}

			// ha nem nyomott semmit, unslide az egesz, meg ha maradt meg ido a frame vegen, akkor is
			if (unslideTime < actFrameStart) unslidePlayer((actFrameStart-unslideTime)>>2);
            if (shipFrostTime >= 0) shipFrostTime -= lastFrameTime;

		// **********************************************************************************************************
		}

		//System.out.println(" >>> repaint()");
        //System.out.flush();
        repaint();
		//System.out.println(" >>> servicerepaints()");
        //System.out.flush();
        serviceRepaints();
		//System.out.println(" >>> sleep()");
        //System.out.flush();
        try {
            Thread.sleep(5);
        } catch (Exception e) {}
		//System.out.println(" >>> callSerially()");
        //System.out.flush();
		myDisplay.callSerially(this);
		//System.out.println(" >>> done()");
        //System.out.flush();

        //Thread.yield();
	}

/*    public void run() {
        repaint();
        //serviceRepaints();
        try {
            Thread.sleep(10);
        } catch (Exception e) {}
		myDisplay.callSerially(this);
		//System.out.println(" >>> run() done");
    }*/

	static void movePlayer(int x, int y, int elapsedTime) {
		if (x != 0) {
			fShipX += x*shipSpeed*elapsedTime;
			if (fShipX < 0) fShipX = 0;
			if (fShipX > fShipLimitX) fShipX = fShipLimitX;

			vscrdelta = (((fShipX*((maxvscrdelta<<16)/fShipLimitX))+32768)>>16);
			fenshift = (((vscrdelta*fEnemyBGSpeed)+127)>>7);
            enshift = (((vscrdelta*fEnemyBGSpeed)+32767)>>15);
			vscrdelta = (vscrdelta<<1) - maxvscrdelta;// - vscrdelta;
			//System.out.println(""+(vscrdelta+maxvscrdelta)+", "+fEnemyBGSpeed+", "+enshift);//+", "+maxvscrdelta+", "+fShipX);

			shipSlideTime += x*elapsedTime;
			if (shipSlideTime > maxShipSlideTime) shipSlideTime = maxShipSlideTime;
			if (shipSlideTime < 0) shipSlideTime = 0;
			//System.out.println("move: "+shipSlideTime);
		}

		if (y != 0) {
			fShipY += y*shipSpeed*elapsedTime;
			if (fShipY < 0) fShipY = 0;
			if (fShipY > fShipLimitY) fShipY = fShipLimitY;
		}
	}

	static void unslidePlayer(int elapsedTime) {
		if (shipSlideTime > maxShipSlideTime/2) {
			shipSlideTime -= elapsedTime;
			if (shipSlideTime < maxShipSlideTime/2) shipSlideTime = maxShipSlideTime/2;
		} else {
			shipSlideTime += elapsedTime;
			if (shipSlideTime > maxShipSlideTime/2) shipSlideTime = maxShipSlideTime/2;
		}
		//System.out.println("unslide: "+shipSlideTime);
	}

    static int nextBulletInd;
    static final void addBullet(boolean enemy, int fx, int fy, int angle, int speed, int imageNum, int damage) {
		while (bullets[nextBulletInd] != null) nextBulletInd++;
		bullets[nextBulletInd] = new Bullet(enemy, fx, fy, angle, speed, imageNum, damage);
    }

    static final void fireTower(boolean enemy, int fx, int fy) {
        int fdx = (fShipX+(fShipWidth>>1)-fx);
        int fdy = (fShipY+(fShipHeight>>1)-fy);        // FIXME: alapbol 5 pixellel az also resze felett indul a lovedek - szukseg lesz korrigalasra
        while (fdx > 32768 || fdy > 32768) {
            fdx >>= 1;
            fdy >>= 1;
        }
        int fd = Stuff.sqrt(fdx*fdx+fdy*fdy);

        // elkerulendo a fagyasokat
        if (fd > 0) {
            addBullet(enemy, fx, fy, -1, 40, 35, 12);
            bullets[nextBulletInd].fdy = fdy;
            bullets[nextBulletInd].fdx = fdx;
            bullets[nextBulletInd].fd = fd;
        }
    }


    static final void fireBoss1(boolean enemy, int fx, int fy) {
      int actShotTime = actFrameStart-mainEnemy.lastShot;
        int phase = ((actShotTime % (80<<8))>>2);       // OPTME: 5-odere kellett lassitani a foellen lovesvaltasait, igy egyszerubb volt, mint mindenhol atirogatni

        // 4
       if ((phase >= (0) && phase < (2<<8)) || (phase >= (5<<8) && phase < (8<<8))|| (phase >= (10<<8) && phase < (12<<8))|| (phase >= (15<<8) && phase < (18<<8))) {
        
    
   
           if ((phase & 64) == 64) {  // nagyplazma kozeprol
                addBullet(enemy, fx, fy, 270, 50, 19, 4);
            }
        }
        // 1
        if ((phase >= (10<<8) && phase < (12<<8))) {
            addBullet(enemy, fx-fBoss1WingOffsetX, fy-fBoss1WingOffsetY, 225, 72, 37, 3);
        }

        // 3
        if ((phase >= (10<<8) && phase < (12<<8))) {
            addBullet(enemy, fx, fy, 225, 72, 37, 3);
        }

        // 5
        if ((phase >= (12<<8) && phase < (14<<8))) {
            addBullet(enemy, fx, fy, 315, 72, 36, 3);
        }

        // 7
        if ((phase > (12<<8) && phase < (14<<8))) {
            addBullet(enemy, fx+fBoss1WingOffsetX, fy-fBoss1WingOffsetY, 315, 72, 36, 3);
        }

        // 2 es 6
        if ((phase >= (8<<8) && phase < (10<<8)) || (phase >= (2<<8) && phase < (5<<8)))
        {
            if (mainEnemyDualLeft) {
                addBullet(enemy, fx-fBoss1WingOffsetX, fy-fBoss1WingOffsetY, 270, 60, 18, 3);
            } else {
                addBullet(enemy, fx+fBoss1WingOffsetX, fy-fBoss1WingOffsetY, 270, 60, 18, 3);
            }
            mainEnemyDualLeft = !mainEnemyDualLeft;
        }

        // 14-20sec kozott jol beszorja a terepet
        if ((phase >= (14<<8)) && (phase < (20<<8))) {
            switch (mainEnemyFront) {
                case 0:
                    addBullet(enemy, fx-fBoss1WingOffsetX, fy-fBoss1WingOffsetY, 225, 72, 37, 3);
                    break;

                case 1:
                    addBullet(enemy, fx, fy, 225, 72, 37, 3);
                    break;

                case 2:
                    addBullet(enemy, fx, fy, 315, 72, 36, 3);
                    break;

                case 3:
                    addBullet(enemy, fx+fBoss1WingOffsetX, fy-fBoss1WingOffsetY, 315, 72, 36, 3);
                    break;
            }

            if (mainEnemyDualLeft) {
                if (mainEnemyFront > 0) {
                    mainEnemyFront--;
                } else {
                    mainEnemyDualLeft = false;
                    mainEnemyFront = 1;     // jobbra megy egyet
                }
            } else {
                if (mainEnemyFront < 3) {
                    mainEnemyFront++;
                } else {
                    mainEnemyDualLeft = true;
                    mainEnemyFront = 2;     // balra megy egyet
                }
            } }
       
    }
    static final void fireBoss2(boolean enemy, int fx, int fy) {
        int actShotTime = actFrameStart-mainEnemy.lastShot;
        int phase = actShotTime % (34<<8);

        // 4
        if (phase < (2<<8) || (phase >= (5<<8) && phase < (8<<8)) || (phase >= (10<<8) && phase < (12<<8)) || (phase >= (15<<8) && phase < (18<<8)) || (phase >= (23<<8) && phase < (24<<8)) || (phase >= (30<<8) && phase < (31<<8))) {
            if ((phase & 64) == 64) {  // nagyplazma kozeprol
                addBullet(enemy, fx, fy, 270, 50, 25, 4);
            }
        }

        // 1
        if ((phase >= (5<<8) && phase < (10<<8)) || (phase >= (10<<8) && phase < (12<<8)) || (phase >= (14<<8) && phase < (16<<8)) || (phase >= (20<<8) && phase < (21<<8)) || (phase >= (33<<8) && phase < (34<<8))) {
            addBullet(enemy, fx-fBoss2WingOffsetX, fy-fBoss2WingOffsetY, 180, 72, 26, 3);
        }

        // 3
        if ((phase >= (5<<8) && phase < (10<<8)) || (phase < (10<<8) && phase < (12<<8)) || (phase >= (14<<8) && phase < (16<<8)) || (phase >= (22<<8) && phase < (23<<8)) || (phase >= (31<<8) && phase < (32<<8))) {
            addBullet(enemy, fx, fy, 225, 72, 20, 3);
        }

        // 5
        if ((phase >= (5<<8) && phase < (10<<8)) || (phase < (12<<8) && phase < (14<<8)) || (phase >= (17<<8) && phase < (20<<8)) || (phase >= (24<<8) && phase < (25<<8)) || (phase >= (29<<8) && phase < (30<<8))) {
            addBullet(enemy, fx, fy, 315, 72, 23, 3);
        }

        // 7
        if ((phase >= (5<<8) && phase < (10<<8)) || (phase < (12<<8) && phase < (14<<8)) || (phase >= (17<<8) && phase < (20<<8)) || (phase >= (27<<8) && phase < (28<<8))) {
            addBullet(enemy, fx+fBoss2WingOffsetX, fy-fBoss2WingOffsetY, 0, 72, 27, 3);
        }

        // 2 es 6
        if (phase < (5<<8)) {
            if (mainEnemyDualLeft) {
                addBullet(enemy, fx-fBoss2WingOffsetX, fy-fBoss2WingOffsetY, 270, 50, 24, 3);
            } else {
                addBullet(enemy, fx+fBoss2WingOffsetX, fy-fBoss2WingOffsetY, 270, 50, 24, 3);
            }
            mainEnemyDualLeft = !mainEnemyDualLeft;
        }

        // 2
        if ((phase >= (21<<8) && phase < (22<<8)) || (phase >= (32<<8) && phase < (33<<8))) {
            addBullet(enemy, fx-fBoss2WingOffsetX, fy-fBoss2WingOffsetY, 270, 50, 24, 3);
        }

        // 6
        if ((phase >= (25<<8) && phase < (26<<8)) || (phase >= (28<<8) && phase < (29<<8))) {
            addBullet(enemy, fx-fBoss2WingOffsetX, fy-fBoss2WingOffsetY, 270, 50, 24, 3);
        }
    }

    static final void fireBoss3(boolean enemy, int fx, int fy) {
        int actShotTime = actFrameStart-mainEnemy.lastShot;
        int phase = actShotTime % (20<<8);

        // 1
        if ((phase >= (2<<8) && phase < (5<<8)) || (phase >= (10<<8) && phase < (12<<8)) || (phase >= (14<<8) && phase < (16<<8))) {
            addBullet(enemy, fx-fBoss3WingOffsetX, fy-fBoss3WingOffsetY, 270, 72, 24, 4);
        }

        // 3
        if (phase < (2<<8) || (phase < (5<<8) && phase < (8<<8)) || (phase >= (10<<8) && phase < (12<<8)) || (phase >= (15<<8) && phase < (18<<8))) {
            addBullet(enemy, fx, fy, 270, 60, 25, 5);
        }

        // 5
        if ((phase >= (2<<8) && phase < (5<<8)) || (phase < (12<<8) && phase < (14<<8)) || (phase >= (16<<8) && phase < (18<<8))) {
            addBullet(enemy, fx+fBoss3WingOffsetX, fy-fBoss3WingOffsetY, 270, 72, 24, 4);
        }

        // 2, 4
        if ((phase >= (5<<8) && phase < (10<<8)) || (phase >= (12<<8) && phase < (14<<8)) || (phase >= (17<<8) && phase < (19<<8))) {
            int fdx = fShipX+(fShipWidth>>1)-fx;
            int fdy = fShipY+(fShipHeight>>1)-fy;        // FIXME: alapbol 5 pixellel az also resze felett indul a lovedek - szukseg lesz korrigalasra
            int fd = Stuff.sqrt(fdx*fdx+fdy*fdy);

            if (mainEnemyDualLeft) {
                addBullet(enemy, fx-fBoss3WingOffsetX, fy-fBoss3WingOffsetY, -1, 50, 35, 5);
            } else {
                addBullet(enemy, fx+fBoss3WingOffsetX, fy-fBoss3WingOffsetY, -1, 50, 35, 5);
            }
            mainEnemyDualLeft = !mainEnemyDualLeft;

            bullets[nextBulletInd].fdy = fdy;
            bullets[nextBulletInd].fdx = fdx;
            bullets[nextBulletInd].fd = fd;
        }
    }

    public static void fireWeapon(boolean enemy, int fx, int fy, int type) {
		//if (enemy) System.out.println("Shoot! "+(fx>>8)+", "+(fy>>8)+", "+type);
        //System.out.flush();
        nextBulletInd = 0;

		switch (type) {
			/************************************** VULCAN */
			case 0:
				addBullet(enemy, fx, fy, 90, 96, 0, 10);
				break;

			case 1:
				if (shipWeaponFrontDualLeft) {
					addBullet(enemy, fx-(4<<8), fy+(3<<8), 90, 96, 0, 10);
				} else {
					addBullet(enemy, fx+(4<<8), fy+(3<<8), 90, 96, 0, 10);
				}
				shipWeaponFrontDualLeft = !shipWeaponFrontDualLeft;
				break;

			case 2:
				addBullet(enemy, fx-(4<<8), fy, 120, 96, 0, 10);
				addBullet(enemy, fx   , fy, 90, 96, 0, 10);
				addBullet(enemy, fx+(4<<8), fy, 60, 96, 0, 10);
				break;

			/************************************** ICE */
			case 3:
				addBullet(enemy, fx, fy, 90, 60, 5, 15);
				break;

			case 4:
				if (shipWeaponFrontDualLeft) {
                    addBullet(enemy, fx-(6<<8), fy, 90, 65, 5, 15);
                } else {
                    addBullet(enemy, fx+(6<<8), fy, 90, 65, 5, 15);
                }
				shipWeaponFrontDualLeft = !shipWeaponFrontDualLeft;
				break;

			case 5:
				addBullet(enemy, fx-(4<<8), fy, 180, 50, 7, 30);
				addBullet(enemy, fx       , fy, 90, 90, 8, 20);
				addBullet(enemy, fx+(4<<8), fy, 0, 50, 6, 20);
				break;

			/************************************** FIRE */
			case 6:
				addBullet(enemy, fx, fy, 90, 70, 1, 40);
				break;

			case 7:
				addBullet(enemy, fx-(6<<8), fy, 90, 70, 1, 25);
				addBullet(enemy, fx+(6<<8), fy, 90, 70, 1, 25);
				break;

			case 8:
				addBullet(enemy, fx-(4<<8), fy, 1, 65, 3, 30);
				addBullet(enemy, fx       , fy, 90, 70, 4, 25);
				addBullet(enemy, fx+(4<<8), fy, 45, 70, 2, 25);
				break;

			/************************************** WAVE */
			case 9:
				addBullet(enemy, fx, fy, 90, 85, 9, 30);
				break;

			case 10:
				addBullet(enemy, fx, fy, 90, 85, 10, 40);
				break;

			case 11:
				addBullet(enemy, fx, fy, 90, 85, 11, 75);
				break;

			/************************************** BACK */
			case 12:
				addBullet(enemy, fx, fy+(shipHeight<<8), 270, 40, 12, 20);
				break;

			case 13:
				if (shipWeaponBackDualLeft) {
					addBullet(enemy, fx, fy+((shipHeight-3)<<8), 300, 45, 12, 10);
				} else {
					addBullet(enemy, fx, fy+((shipHeight-3)<<8), 240, 45, 12, 10);
				}
				shipWeaponBackDualLeft = !shipWeaponBackDualLeft;
				break;

			case 14:
				addBullet(enemy, fx, fy+(shipHeight<<8), 270, 50, 13, 20);
				break;

			case 15:
				addBullet(enemy, fx, fy+(shipHeight<<8), 270, 50, 13, 20);
				break;
	/************************************** ENEMY - KEKPLAZMA */
            case 16:
				addBullet(enemy, fx-(3<<8), fy, 225, 60, 20, 2);
                addBullet(enemy, fx-(1<<8), fy, 255, 60, 18, 2);
				addBullet(enemy, fx+(1<<8), fy, 285, 60, 18, 2);
                addBullet(enemy, fx+(3<<8), fy, 315, 60, 23, 2);
                break;

            case 17:
				addBullet(enemy, fx, fy, 270, 80, 19, 16);
                break;

            case 18:
				addBullet(enemy, fx-(3<<8), fy, 225, 60, 20, 6);
				addBullet(enemy, fx-(1<<8), fy, 255, 60, 18, 6);
				addBullet(enemy, fx+(1<<8), fy, 285, 60, 18, 6);
				addBullet(enemy, fx+(3<<8), fy, 315, 60, 23, 6);
                fy -= 8<<8;
				addBullet(enemy, fx-(3<<8), fy, 135, 50, 21, 6);
				addBullet(enemy, fx+(3<<8), fy, 45, 50, 22, 6);
                break;

			/************************************** ENEMY - RAKETA */
            case 19:
                fy -= 6<<8;
				addBullet(enemy, fx-(5<<8), fy, 270, 50, 24, 10);
                bullets[nextBulletInd].fPhase = 0;
				addBullet(enemy, fx+(5<<8), fy, 270, 50, 24, 10);
                bullets[nextBulletInd].fPhase = 90<<8;
                break;

            case 20:
                fy -= 2<<8;
				addBullet(enemy, fx, fy, 244+(random.nextInt()&63), 50, 24, 6);
                break;

            case 21:
				addBullet(enemy, fx, fy, 270, 65, 18, 2);
                fy -= 6<<8;
				addBullet(enemy, fx+(5<<8), fy, 285, 50, 24, 14);
				addBullet(enemy, fx-(5<<8), fy, 255, 50, 24, 14);
                break;

			/************************************** ENEMY - TUZLABDA */
            case 22:
				addBullet(enemy, fx, fy, 270, 60, 28, 4);
                break;

            case 23:
				addBullet(enemy, fx-(4<<8), fy, 270, 60, 25, 8);
				addBullet(enemy, fx+(4<<8), fy, 270, 60, 25, 8);
                break;

            case 24:
				addBullet(enemy, fx-(3<<8), fy, 240, 50, 28, 4);
				addBullet(enemy, fx, fy, 270, 30, 28, 4);
				addBullet(enemy, fx+(3<<8), fy, 300, 50, 28, 4);
				addBullet(enemy, fx, fy, 180, 30, 26, 12);
				addBullet(enemy, fx, fy, 0, 30, 27, 12);
                break;

			/************************************** ENEMY - FROST */
            case 25:
				addBullet(enemy, fx, fy, 270, 50, 29, 130);
                break;

            case 26:
				addBullet(enemy, fx, fy, 270, 50, 31, 200);
                break;

            case 27:
				addBullet(enemy, fx, fy, 270, 30, 30, 128);
				addBullet(enemy, fx-(3<<8), fy, 225, 80, 32, 64);
				addBullet(enemy, fx+(3<<8), fy, 315, 80, 33, 64);
                break;

            /************************************** ENEMY - TOWER */
            case 28:
                fireTower(enemy, fx, fy);
                break;

            /************************************** ENEMY - BOSS1 */
            case 29:
                fireBoss1(enemy, fx, fy);
                break;

            /************************************** ENEMY - BOSS2 */
            case 30:
                fireBoss2(enemy, fx, fy);
                break;

            /************************************** ENEMY - BOSS3 */
            case 31:
                fireBoss3(enemy, fx, fy);
                break;
                
           case 32:
				
                addBullet(enemy, fx-(1<<8), fy, 270, 40, 18, 2);
		
     
                break;
                
           case 33:
				addBullet(enemy, fx-(6<<8), fy, 270, 60, 18, 2);
				addBullet(enemy, fx+(6<<8), fy, 270, 60, 18, 2);
                break;
         
           case 34:
				addBullet(enemy, fx, fy, 270, 60, 25, 8);
			
                break;
        }
        //if (enemy) System.out.println("done.");
        //System.out.flush();

        nextBulletInd++;
		if (bulletLast < nextBulletInd) bulletLast = nextBulletInd;
	}

/**********************************************************************************
/* MENU
/**********************************************************************************/
    public static void moveMenu(int key) {	// kizarolag state_menu alatt hivodik meg
		switch (key) {
			case Canvas.DOWN:
				menuSelect++;
                if (menuSelect >= 5) menuSelect -= 5;
				while (menu[menuSelect+menuOffset] < 0) {
                    menuSelect++;
                    if (menuSelect >= 5) menuSelect -= 5;
                }
				break;

			case Canvas.UP:
				menuSelect--;
                if (menuSelect < 0) menuSelect += 5;
				while (menu[menuSelect+menuOffset] < 0) {
                    menuSelect--;
                    if (menuSelect < 0) menuSelect += 5;
                }
				break;

            case Canvas.KEY_STAR:
                // back - ejj be ruut
                {
                    for (int i = 0; i < 5; i++) {
                        if ((menuJump[i+menuOffset] >= 0) && (menuJump[i+menuOffset] < menuOffset)) {
        					if (menuParent >= 0) menuSelect = menuParent%5;
                            else menuSelect = 0;
                            
                            // az upgrade menubol visszateres kulon elbanasban reszesul
                            if (i+menuOffset == 24) state = STATE_MENU_UPGRADE_CLOSE;
                            if (i+menuOffset == 29) state = STATE_WEAPONINFO_OUT;
                            
        					menuParent = -1;
        					menuOffset = menuJump[i+menuOffset];
                            break;
                        }
                    }
                }
                break;

            case Canvas.KEY_POUND:
            case Canvas.FIRE:
				switch (menuSelect+menuOffset) {
					case 2:			// control
                        initStory(8);
						state = STATE_MENU_TO_STORY;
						break;
						
					case 3:			// about
                        initStory(7);
						state = STATE_MENU_TO_STORY;
						break;

					case 4:			// exit
                        nextState = STATE_EXIT;
                        state = STATE_DISSOLVE_PAR;
						break;

                    case 5:         // new game
                        // EVENT: new game
                        if (menu[6] > 0) {
                            state = STATE_MENU_NEWQUESTION;
                        } else {
                            resetGame();
                        }
                        break;

                    case 6:         // continue
                        loadGame();
                        //state = STATE_MENU_DOOR_IN;
                        break;

                    case 7:         // high scores
                        state = STATE_MENU_TO_SCORES;
                        break;

					case 10:		// start game
						state = STATE_MENU_CHAPTER_OPEN;
						selectedChapter = 0;
						chapterBigImageX = 0;
						accMenuTime = 0;
						break;

					case 11:	// upgrade ship
						shopWeaponFront = shipWeaponFront;
						shopWeaponBack = shipWeaponBack;
						shopWeaponShield = shipWeaponShield;
						shopWeaponExtra = shipWeaponExtra;
						initShield(shopWeaponShield);
						state = STATE_MENU_UPGRADE_OPEN;
						break;

					case 20:	// front/rear/shield/extra menube belepes - weaponinfo megjelenites
					case 21:
					case 22:
					case 23:
						shopWeaponFront = shipWeaponFront;
						shopWeaponBack = shipWeaponBack;
						shopWeaponShield = shipWeaponShield;
						shopWeaponExtra = shipWeaponExtra;
						state = STATE_WEAPONINFO_IN;
						break;

					case 29:	// front/rear/shield/extra menubol kilepes - weaponinfo eltuntetes
						shopWeaponFront = shipWeaponFront;
						shopWeaponBack = shipWeaponBack;
						shopWeaponShield = shipWeaponShield;
						shopWeaponExtra = shipWeaponExtra;
						initShield(shopWeaponShield);
						state = STATE_WEAPONINFO_OUT;
						break;

					case 24:	// back from upgrade
						state = STATE_MENU_UPGRADE_CLOSE;
						break;

                    case 27:
                        switch (menuParent) {
                            case 20:
                                if (shopWeaponFront >= 0 && score >= weaponPrice[shopWeaponFront]) {
                                    score -= weaponPrice[shopWeaponFront];
                                    int newWeapon = shopWeaponFront;
                                    if (shipWeaponFront >= 0) sellWeapon(menuParent);
                                    shipWeaponFront = shopWeaponFront = newWeapon;
                                } else state = STATE_NOT_ENOUGH_MONEY;
                                break;

                            case 21:
                                if (shopWeaponBack >= 0 && score >= weaponPrice[shopWeaponBack]) {
                                    score -= weaponPrice[shopWeaponBack];
                                    int newWeapon = shopWeaponBack;
                                    if (shipWeaponBack >= 0) sellWeapon(menuParent);
                                    shipWeaponBack = shopWeaponBack = newWeapon;
                                } else state = STATE_NOT_ENOUGH_MONEY;
                                break;

                            case 22:
                                if (shopWeaponShield >= 0 && score >= weaponPrice[shopWeaponShield+16]) {
                                    score -= weaponPrice[shopWeaponShield+16];
                                    int newWeapon = shopWeaponShield;
                                    if (shipWeaponShield >= 0) sellWeapon(menuParent);
                                    shipWeaponShield = shopWeaponShield = newWeapon;
                                    initShield(shopWeaponShield);
                                } else state = STATE_NOT_ENOUGH_MONEY;
                                break;

                            case 23:
                                if (shopWeaponExtra >= 0 && score >= weaponPrice[shopWeaponExtra+18]) {
                                    score -= weaponPrice[shopWeaponExtra+18];
                                    int newWeapon = shopWeaponExtra;
                                    if (shipWeaponExtra >= 0) sellWeapon(menuParent);
                                    shipWeaponExtra = shipWeaponExtra = newWeapon;
                                } else state = STATE_NOT_ENOUGH_MONEY;
                                break;
                        }
                        break;

                    case 28:
                        sellWeapon(menuParent);
                        break;

					case 25:	// upgrade - next
						switch (menuParent) {
							case 20:	// front
								shopWeaponFront++;
								if (shopWeaponFront >= 11) shopWeaponFront = 0;
								break;

							case 21:	// back
								shopWeaponBack++;
								if (shopWeaponBack >= 16 || shopWeaponBack < 12) shopWeaponBack = 12;
								break;

							case 22:	// shield
								shopWeaponShield++;
								if (shopWeaponShield >= 2 || shopWeaponShield < 0) shopWeaponShield = 0;
								initShield(shopWeaponShield);
								break;

							case 23:	// extra
								shopWeaponExtra++;
								if (shopWeaponExtra >= 3 || shopWeaponExtra < 0) shopWeaponExtra = 0;
								break;
						}
						break;

					case 26:	// upgrade - prev
						switch (menuParent) {
							case 20:	// front
								shopWeaponFront--;
								if (shopWeaponFront < 0) shopWeaponFront = 10;
								break;

							case 21:	// back
								shopWeaponBack--;
								if (shopWeaponBack < 12) shopWeaponBack = 15;
								break;

							case 22:	// shield
								shopWeaponShield--;
								if (shopWeaponShield < 0) shopWeaponShield = 1;
								initShield(shopWeaponShield);
								break;

							case 23:	// extra
								shopWeaponExtra--;
								if (shopWeaponExtra < 0) shopWeaponExtra = 2;
								break;
						}
						break;
						
					case 30:	// sound on
						musicOn = true;
						startMidi();
						break;

					case 31:	// sound off
						musicOn = false;
						stopMidi();
						break;

					case 35:	// difficulty easy
						menu[32] = 13;
						break;

					case 36:	// difficulty normal
						menu[32] = 14;
						break;

					case 37:	// difficulty hard
						menu[32] = 15;
						break;
						
				}

				if (menuJump[menuSelect+menuOffset] >= 0) {
					menuParent = menuOffset + menuSelect;
					menuOffset = menuJump[menuSelect+menuOffset];
					menuSelect = 0;
				}
				break;
		}
	}

    static void sellWeapon(int menuParent) {
        switch (menuParent) {
            case 20:
                if (shipWeaponFront >= 0) {
                    score += weaponPrice[shipWeaponFront];
                    shipWeaponFront = shopWeaponFront = -1;
                }
                break;

            case 21:
                if (shipWeaponBack >= 0) {
                    score += weaponPrice[shipWeaponBack];
                    shipWeaponBack = shopWeaponBack = -1;
                }
                break;

            case 22:
                if (shipWeaponShield >= 0) {
                    score += weaponPrice[16+shipWeaponShield];
                    shipWeaponShield = shopWeaponShield = -1;
                    initShield(shopWeaponShield);
                }
                break;

            case 23:
                if (shipWeaponExtra >= 0) {
                    score += weaponPrice[18+shipWeaponExtra];
                    shipWeaponExtra = shopWeaponExtra = -1;
                }
                break;
        }
    }

	static void paintMenu(Graphics g) {
        int menuStartX = scrX-rightDoorWidth;
        int menuPointHeight = actMenuHeight/5;
        int menuStartY = topHeight+menuPointYOffset;

		g.setClip(0, 0, scrX, scrY);
		g.drawImage(menuBG, menuStartX, topHeight, Graphics.TOP|Graphics.LEFT);
		g.setClip(menuStartX, menuStartY+(menuPointHeight*menuSelect), rightDoorWidth, menuPointHeight);
		g.drawImage(menuBGAct, menuStartX, menuStartY, Graphics.TOP|Graphics.LEFT);
        
        if (prevState == STATE_MENU_NEWQUESTION) {
            prevState = -1;
            animateLeftDoor(g, 256);
        }

		for (int i = 0; i < 5; i++) {
			int mess = menu[menuOffset+i];
			if (mess >= 0) writeStringCentered(g, (menuStartX+scrX)>>1, menuStartY+((menuPointHeight-fontHeight)>>1)+(i*menuPointHeight), messages[mess]);
		}
	}

	static void paintUpgrade(Graphics g) {
		// hatter
		g.setClip(0, 0, scrX, scrY);
		g.drawImage(upgradeBG, 0, topHeight, Graphics.TOP|Graphics.LEFT);

		int topLimit = topHeight;
		if (topScorePos == top1Height) {
			animateTopScore(g, 256);
			writeNumRight(g, top1Width-top1NumXOffset, top1NumYOffset, score);
			topLimit = Math.max(topHeight, top1Height);
		}

		int areaBottom = pbottom - weaponInfoPos;

		if (weaponInfoPos > 0) {		// fegyver infok kellenek
			g.setClip(0, ptop, scrX, pheight);
			g.drawImage(weaponInfoImage, 0, areaBottom, Graphics.TOP|Graphics.LEFT);

			if (weaponInfoPos == weaponInfoHeight) {
				switch (menuParent) {
					case 20:	// front
						if (shopWeaponFront >= 0) {
							writeStringLeft(g, storyMargin>>1, areaBottom+weaponInfo1YOffset, messages[weaponNameIndex+(shopWeaponFront/3)]);
							writeShop(g, leftDoorWidth, areaBottom+weaponInfo2YOffset, weaponPrice[shopWeaponFront]);
							writeShop(g, leftDoorWidth, areaBottom+weaponInfo3YOffset, (shopWeaponFront%3)+1);
						} else {
							writeStringLeft(g, storyMargin>>1, areaBottom+weaponInfo1YOffset, messages[weaponNameIndex-1]);
						}
						break;

					case 21:	// rear
						if (shopWeaponBack >= 12) {
							writeStringLeft(g, storyMargin>>1, areaBottom+weaponInfo1YOffset, messages[weaponNameIndex+4+((shopWeaponBack-12)>>1)]);
							writeShop(g, leftDoorWidth, areaBottom+weaponInfo2YOffset, weaponPrice[shopWeaponBack]);
							writeShop(g, leftDoorWidth, areaBottom+weaponInfo3YOffset, (shopWeaponBack&1)+1);
						} else {
							writeStringLeft(g, storyMargin>>1, areaBottom+weaponInfo1YOffset, messages[weaponNameIndex-1]);
						}
						break;

					case 22:	// shield
						if (shopWeaponShield >= 0) {
							writeStringLeft(g, storyMargin>>1, areaBottom+weaponInfo1YOffset, messages[weaponNameIndex+6]);
							writeShop(g, leftDoorWidth, areaBottom+weaponInfo2YOffset, weaponPrice[shopWeaponShield+16]);
							writeShop(g, leftDoorWidth, areaBottom+weaponInfo3YOffset, shopWeaponShield+1);
						} else {
							writeStringLeft(g, storyMargin>>1, areaBottom+weaponInfo1YOffset, messages[weaponNameIndex-1]);
						}
						break;

					case 23:	// extra
						if (shopWeaponExtra >= 0) {
							writeStringLeft(g, storyMargin>>1, areaBottom+weaponInfo1YOffset, messages[weaponNameIndex+7]);
							writeShop(g, leftDoorWidth, areaBottom+weaponInfo2YOffset, weaponPrice[shopWeaponExtra+18]);
							writeShop(g, leftDoorWidth, areaBottom+weaponInfo3YOffset, shopWeaponExtra+1);
						} else {
							writeStringLeft(g, storyMargin>>1, areaBottom+weaponInfo1YOffset, messages[weaponNameIndex-1]);
						}
						break;
				}
			}
		}
		int fShipX = ((leftDoorWidth/2) - (shipWidth/2))<<8;
		int fShipY = (topHeight<<8)+((areaBottom-topHeight)<<7);
		int fShipMiddleX = fShipX+(fShipWidth>>1);
		int fShipMiddleY = fShipY+(fShipHeight>>1);

		// shield move
		if (shopWeaponShield >= 0) {
			int ad = (lastFrameTime*90+128)>>8;
			for (int i = 0; i <= shopWeaponShield; i++) {
				Bullet b = bullets[shieldBulletIndex[i]];
				b.angle += ad;
				while (b.angle >= 360) b.angle -= 360;
				b.fxAct = fShipMiddleX+((Stuff.cos(b.angle)*fShipWidth)>>16)-(fBulletWidth[14]>>1);		// optme: ezt beirni konstansra
				b.fyAct = fShipMiddleY+((Stuff.sin(b.angle)*fShipHeight)>>16)-(fBulletHeight[14]>>1);
			}
		}

		// move & draw & delete bullets
		int actLastBullet = 0;
		for (int i = 0; i < bulletLast; i++) {
			Bullet b = bullets[i];
			if (b != null) {
				b.move(lastFrameTime);

				// draw
				int actX = b.fxAct>>8;
				int actY = b.fyAct>>8;
				int type = b.type;

				// palyan van-e meg
				if (actX+bulletWidth[type] <= 0 || actX >= leftDoorWidth || actY+bulletHeight[type] <= topLimit || actY >= areaBottom) {
					if (b.type != 14) bullets[i] = null;
					continue;
				}

				g.setClip(actX, actY, bulletWidth[type], bulletHeight[type]);
				g.clipRect(0, topLimit, leftDoorWidth, areaBottom-topLimit);
				g.drawImage(bulletImages[type], actX-(b.actFrame*bulletWidth[type]), actY, Graphics.TOP|Graphics.LEFT);
				actLastBullet = i;
			}
		}
		bulletLast = actLastBullet+1;

		if (shopWeaponFront >= 0 && shopWeaponFront < 12) {
			shipFrontFired += lastFrameTime;
			while (shipFrontFired > weaponRate[shopWeaponFront]) {
				shipFrontFired -= weaponRate[shopWeaponFront];
				fireWeapon(false, fShipMiddleX, fShipY, shopWeaponFront);
			}
		}

		if (shopWeaponBack >= 12 && shopWeaponBack < 16) {
			shipBackFired += lastFrameTime;
			while (shipBackFired > weaponRate[shopWeaponBack]) {
				shipBackFired -= weaponRate[shopWeaponBack];
				fireWeapon(false, fShipMiddleX, fShipY-(4<<8), shopWeaponBack);
			}
		}

		if (shopWeaponExtra >= 0) {
			int actBullet = 15+shopWeaponExtra;
			int actWeapon = shopWeaponExtra*3;
			fExtraGunX = fShipX+fShipWidth;
			fExtraGunY = fShipY+fShipHeight-fBulletHeight[actBullet]-extraGunVerticalTilt[shopWeaponExtra];

			g.setClip(0, 0, scrX, scrY);
			g.drawImage(bulletImages[actBullet], fExtraGunX>>8, fExtraGunY>>8, Graphics.TOP|Graphics.LEFT);

			shipExtraFired += lastFrameTime;
			while (shipExtraFired > weaponRate[actWeapon]) {
				shipExtraFired -= weaponRate[actWeapon];
				fireWeapon(false, fExtraGunX+(fBulletWidth[actBullet]>>1), fExtraGunY, actWeapon);
			}
		}

		// draw ship
		int actShipX = fShipX>>8;
		int actShipY = fShipY>>8;
		g.setClip(actShipX, actShipY, shipWidth, shipHeight);
		g.drawImage(shipImage, actShipX-(3*shipWidth), actShipY, Graphics.TOP|Graphics.LEFT);
	}

    static int editingHighScore = -1, editingHighScorePos = 0;
    static char[] editingName;

    static void paintHighScore(Graphics g, int accMenuTime) {
        g.setClip(0, ptop, scrX, pbottom);
        writeStringCentered(g, scrX>>1, ptop+storyMargin, messages[weaponNameIndex+8]);
        int scoresBegin = ptop+storyMargin+((fontHeight+storyMargin)<<1);

        for (int i = 0; i < 8 && highScoreNames[i] != null; i++) {
            if (highScorePossible && highScores[i] < score) {
                highScorePossible = false;
                editingHighScore = i;
                editingHighScorePos = 0;
                if (i < 7) {
                    System.arraycopy(highScores, i, highScores, i+1, 7-i);
                    System.arraycopy(highScoreNames, i, highScoreNames, i+1, 7-i);
                }
                highScores[i] = score;
                editingName = "a       ".toCharArray();

                // reinit
                resetGame();
            }

            if (editingHighScore == i) {
                String actName = new String(editingName);
                if ((accMenuTime & 64) > 0) {
                    int xPos = getStringWidth(actName.substring(0, editingHighScorePos+1));
                    writeStringLeft(g, storyMargin, scoresBegin, actName.substring(0, editingHighScorePos));
                    writeStringLeft(g, storyMargin+xPos, scoresBegin, actName.substring(editingHighScorePos+1));
                } else {
                    writeStringLeft(g, storyMargin, scoresBegin, actName);
                }
            } else {
                writeStringLeft(g, storyMargin, scoresBegin, highScoreNames[i]);
            }
            writeStringRight(g, scrX-(storyMargin), scoresBegin, Integer.toString(highScores[i]));

            scoresBegin += fontHeight + (storyMargin>>1);
        }
        if (highScorePossible) {
            // reinit
            resetGame();
            highScorePossible = false;
        }
    }

    static void paintStoryPage(Graphics g, int xshift, int page) {
        String s = messages[storyMessageIndex+actStory];
        int lastIndex = s.length();
        int xPos = storyMargin+xshift;
        int yPos = ptop+storyMargin;

        int indstart = storyPageIndex[page];
        int indend = getNextLine(s, indstart, storyWidth);

        while (true) {
            String actline = s.substring(indstart, indend).trim();
            writeStringLeft(g, xPos, yPos, actline);

            if (indend >= lastIndex) {  // story vege
                lastStoryPage = Math.max(actStoryPage, prevStoryPage);
                break;
            }

            yPos += fontHeight+storyMargin;
            if (yPos > pbottom-storyMargin-fontHeight) {
                storyPageIndex[actStoryPage+1] = indend+1;
                break;
            }

            indstart = indend;
            indend = getNextLine(s, indstart+1, storyWidth);
        }
    }

    static void paintStory(Graphics g, int accMenuTime) {
        //g.setColor(0);
        //g.fillRect(0, ptop, scrX, pheight);
        //g.drawImage(startImage, 0, 0, Graphics.TOP|Graphics.LEFT);
        //animateStoryBackground(g, accMenuTime);
        g.setClip(0, ptop, scrX, pheight);

        if (accMenuTime > 256 || accMenuTime < 0) {    // csak az aktualisat kell kirakni
            paintStoryPage(g, 0, actStoryPage);

        } else {
    		int separatorPos = ((accMenuTime*accMenuTime*scrX)+32768)>>16;

            // melyik iranyba mozogjon
            if (prevStoryPage < actStoryPage) {     // jobbra lepunk, balra csuszunk
                paintStoryPage(g, scrX-separatorPos, actStoryPage);
                paintStoryPage(g, -separatorPos, prevStoryPage);

            } else {    // balra lepunk, jobbra csuszunk
                paintStoryPage(g, separatorPos-scrX, actStoryPage);
                paintStoryPage(g, separatorPos, prevStoryPage);
            }
        }

    }

	// a felso keretet mozgatja
	static void animateTopBorder(Graphics g, int accMenuTime) {
		topBorderPos = accMenuTime*topHeight;
		g.setClip(0, 0, scrX, scrY);
		g.drawImage(topBorder, 0, ((topBorderPos+128)>>8)-topHeight, Graphics.TOP|Graphics.LEFT);
	}

	// a menut elfedo ajto mozgatasa
	static void animateRightDoor(Graphics g, int doorTime) {
		rightDoorPos = (doorTime*doorTime*rightDoorWidth)>>8;
		g.setClip(0, 0, scrX, scrY);
		g.drawImage(doorImage, scrX-((rightDoorPos+128)>>8), topHeight, Graphics.TOP|Graphics.LEFT);
	}

	static void animateLeftDoor(Graphics g, int doorTime) {
		leftDoorPos = (doorTime*doorTime*leftDoorWidth)>>8;
		g.setClip(0, 0, scrX, scrY);
		g.drawImage(doorImage, ((leftDoorPos+128)>>8)-leftDoorWidth, topHeight, Graphics.TOP|Graphics.LEFT);
	}

	static void animateMenuAsDoor(Graphics g, int doorTime) {
		rightDoorPos = (doorTime*doorTime*rightDoorWidth)>>8;
		g.setClip(0, 0, scrX, scrY);
		g.drawImage(menuBG, scrX-((rightDoorPos+128)>>8), topHeight, Graphics.TOP|Graphics.LEFT);
	}

    // a score es a lives mozgatasa
	static void animateTopLives(Graphics g, int topTime) {
		topLivesPos = (topTime*top1Height+128)>>8;

		g.setClip(0, 0, scrX, scrY);
		g.drawImage(topLives, scrX-top3Width, topLivesPos-top1Height, Graphics.TOP|Graphics.LEFT);
	}

	static void animateTopScore(Graphics g, int topTime) {
		topScorePos = (topTime*top1Height+128)>>8;

		g.setClip(0, 0, scrX, scrY);
		g.drawImage(topScore, 0, topScorePos-top1Height, Graphics.TOP|Graphics.LEFT);
	}

	static void animateBottomDisplay(Graphics g, int accMenuTime) {
		bottomBorderPos = accMenuTime*bottomHeight;
		bottomLeftDisplayPos = accMenuTime*bottom1Width;
		bottomRightDisplayPos = accMenuTime*bottom3Width;

		g.setClip(0, 0, scrX, scrY);
		g.drawImage(bottomBorder, bottom1Width, scrY-((bottomBorderPos+128)>>8), Graphics.TOP|Graphics.LEFT);
		g.drawImage(bottomLeftDisplay, ((bottomLeftDisplayPos+128)>>8)-bottom1Width, scrY-bottomHeight, Graphics.TOP|Graphics.LEFT);
		g.drawImage(bottomRightDisplay, scrX-((bottomRightDisplayPos+128)>>8), scrY-bottomHeight, Graphics.TOP|Graphics.LEFT);
	}

	static void animateBottomMenuIcons(Graphics g, int accMenuTime, Image leftIcon, Image rightIcon) {
		g.setClip(0, 0, scrX, scrY);

		if (leftIcon != null) {
    		bottomLeftIconPos = (accMenuTime*accMenuTime*bottom1Width)>>7;
            g.drawImage(leftIcon, ((bottomLeftIconPos+128)>>8)-((bottom1Width<<1)-bottom1ButtonXOffset), scrY-bottomHeight+bottomButtonYOffset, Graphics.TOP|Graphics.LEFT);
        }

		if (rightIcon != null) {
    		bottomRightIconPos = (accMenuTime*accMenuTime*bottom3Width)>>7;
            int xPos = scrX+bottom3Width-((bottomRightIconPos+128)>>8)+bottom3ButtonXOffset;
            if (rightIcon == bottomHP) {
                int hpLen = (shipHP*((button3Width<<8)/maxShipHP)+128)>>8;
                if (hpLen == 0) return;
                g.setClip(xPos, scrY-bottomHeight+bottomButtonYOffset, hpLen, bottomHP.getHeight());
            }
            g.drawImage(rightIcon, xPos, scrY-bottomHeight+bottomButtonYOffset, Graphics.TOP|Graphics.LEFT);
        }
	}

	static void animateWeaponInfo(Graphics g, int accMenuTime) {
		weaponInfoPos = (accMenuTime*accMenuTime*weaponInfoHeight+32768)>>16;

		// a kirajzolast a paintUpgrade() vegzi
		//g.setClip(0, ptop, scrX, pheight);
		//g.drawImage(weaponInfoImage, 0, pbottom-weaponInfoPos, Graphics.TOP|Graphics.LEFT);
	}

	static byte[] dissolveBitfield = null;
	static int dissolvedPixels = 0;
    static int scrDissolvePixels, scrDissolvePixelX;
    static final int scrDissolveSizePixels = 1<<scrDissolveSize;
    static int dissolveYOffset = 0, dissolveScrY = -1;

	static void animateDissolve(Graphics g, int accMenuTime) {
		g.setColor(0, 0, 0);
		g.setClip(0, 0, scrX, scrY);

		if (dissolveBitfield == null) {
            scrDissolvePixels = (scrX*dissolveScrY)>>(scrDissolveSize+scrDissolveSize);
            scrDissolvePixelX = scrX>>scrDissolveSize;
            dissolveBitfield = new byte[(scrDissolvePixels>>3)+1];
			dissolvedPixels = 0;
		}
        int toDissolvePixel = (accMenuTime*scrDissolvePixels)>>8;

		for (; dissolvedPixels < toDissolvePixel; dissolvedPixels++) {
            int next = 0;
			int time = 3;
			do {
				if (time > 0) {
					next = Math.abs(random.nextInt()) % scrDissolvePixels;
					time--;
				} else {
					next++;
					if (next >= scrDissolvePixels) next = 0;
				}
			} while ((dissolveBitfield[next>>3] & (1<<(next&7))) > 0);

			dissolveBitfield[next>>3] |= (1<<(next&7));

            int xPos = (next % scrDissolvePixelX) << scrDissolveSize;
            int yPos = (next / scrDissolvePixelX) << scrDissolveSize;
            g.fillRect(xPos, yPos + dissolveYOffset, scrDissolveSizePixels, scrDissolveSizePixels);
		}
	}

    static Image[] storyImages = null;
    static int[] storyBG = null;
    static int storyBGX, storyBGY;
    static int prevStoryBGMod, prevStoryBGDiv;
    static boolean storyBGPlanet;
    static int accStoryBGTime;

    static void loadStoryImages() {
        prevStoryBGMod = prevStoryBGDiv = 0;
        storyBGPlanet = false;
        accStoryBGTime = 0;

        storyImages = new Image[4];
        try {
            storyImages[0] = Image.createImage("/bg/b00.png");
            storyImages[1] = Image.createImage("/bg/b01.png");
            storyImages[2] = Image.createImage("/bg/b02.png");
            storyImages[3] = Image.createImage("/bg/b07.png");
        } catch (Exception e) {}
        storyBGX = ((pwidth+31)>>5)+1;
        storyBGY = ((pheight+31)>>5);
        storyBG = new int[storyBGX*storyBGY];
        for (int i = 0; i < storyBG.length; i++) {
            storyBG[i] = Math.abs(random.nextInt())%3;
        }
    }

    static void disposeStoryImages() {
        storyImages = null;
    }

  /*  static void paintStoryBackground(Graphics g, int actTime) {
        accStoryBGTime += actTime;
        g.setClip(0, ptop, scrX, pheight);
        int modshift = 0, divshift = 0;

        if (accMenuTime < 0) {
            modshift = prevStoryBGMod;
            divshift = prevStoryBGDiv;

        } else {
            modshift = (accStoryBGTime&0x1ff)>>4;
            divshift = (accStoryBGTime>>9) % storyBGX;

            if (divshift > prevStoryBGDiv) {
                int colUpdate = divshift-1;
                if (colUpdate < 0) colUpdate += storyBGX;
                if (colUpdate >= storyBGX) colUpdate -= storyBGX;

                int ind = (colUpdate)*storyBGY;

                for (int i = 0; i < storyBGY; i++) {
                    int modValue = storyBGPlanet?3:4;
                    if (storyBG[ind+i] == 3) {
                        storyBGPlanet = false;
                    }
                    int newsq = Math.abs(random.nextInt())%modValue;
                    if (newsq == 3) storyBGPlanet = true;
                    storyBG[ind+i] = newsq;
                }
            }

            prevStoryBGDiv = divshift;
            prevStoryBGMod = modshift;
        }

        for (int i = 0; i < storyBGX; i++) {
            int xPos = (i<<5)-modshift;
            int xInd = divshift*storyBGY;

            for (int j = 0; j < storyBGY; j++) {
                int tileInd = storyBG[xInd+j];
                g.drawImage(storyImages[tileInd], xPos, ptop+(j<<5), Graphics.TOP|Graphics.LEFT);
            }

            divshift++;
            if (divshift >= storyBGX) divshift -= storyBGX;
        }
    }
*/


    static void paintStoryBackground(Graphics g, int actTime) {
      accStoryBGTime += actTime;
      g.setClip(0, ptop, scrX, pheight);
      g.setColor(0, 0, 0);
      g.fillRect(0, ptop, scrX, pheight);
}

    
/**********************************************************************************
/* STATE MACHINE
/**********************************************************************************/
    public static final void clearScreen(Graphics g) {
        g.setClip(0, 0, realscrX+64, realscrY+128);
        g.setColor(0, 0, 0);
        g.fillRect(0, 0, realscrX+64, realscrY+128);
    }

    public static final void displayImageCentered(Graphics g, String file) {
        try {
            clearScreen(g);
            Image actIm = Image.createImage(file);
            int xPos = (scrX - actIm.getWidth())>>1;
            int yPos = (scrY - actIm.getHeight())>>1;
            dissolveYOffset = Math.min(0, yPos);
            dissolveScrY = Math.max(scrY, actIm.getHeight());
            g.drawImage(actIm, xPos, yPos, Graphics.TOP|Graphics.LEFT);
        } catch (Exception e) {}
    }

    public static final void stopMidi() {
    	if (player == null) return;
    	try {
    		player.stop();
			player.close();
		} catch (Exception e) {}
		player = null;
    }
    
    public static final void startMidi() {
        if (actMusic == null || player != null) return;
		try {
			player = Manager.createPlayer(myClass.getResourceAsStream(actMusic), "audio/midi");
			player.setLoopCount(-1);
			player.start();
		} catch (Exception e) {
		}
    }

    public static final void playMidi(String file) {
        if (file.equals(actMusic)) return;
        actMusic = file;

		if (musicOn) {
			stopMidi();
			startMidi();
		}
    }

    public static final void playSound(int sound) {
        try {
            //soundPlayer[sound].setMediaTime(0);
            //soundPlayer[sound].start();
        } catch (Exception e) {}
    }

    public void paint(Graphics g) {
        paintMain(g);
    }

    public static final void paintMain(Graphics g) {
        //System.out.print("PaintMain; state: "+state+"; accMenuTime: "+accMenuTime+"; lastFrameTime: "+lastFrameTime);
        //System.out.flush();
		switch (state) {
			case STATE_LOGO:
                if (accMenuTime == 0) {
                    displayImageCentered(g, "/ge.png");
                    accMenuTime++;
                } else if (messages == null) {
                    loadStatic();
                }

                accMenuTime += lastFrameTime;
                if (accMenuTime > 320) {
                    clearScreen(g);
                    dissolveBitfield = null;
                    accMenuTime = 0;
                    state = STATE_START;
                } else if (accMenuTime > 192) {
                    animateDissolve(g, (accMenuTime-192)<<1);
                }
                break;

            case STATE_START:
                if (accMenuTime == 0) {
                    displayImageCentered(g, "/start.png");
                    accMenuTime++;
                } else if (menuBG == null) {
                    loadMenu(true);
                    playMidi("/menu.mid");
                }

                accMenuTime += lastFrameTime;
                if (accMenuTime > 256) {
                    accMenuTime = -lastFrameTime;
                    clearScreen(g);
                    dissolveBitfield = null;
                    dissolveYOffset = 0;
                    dissolveScrY = scrY;
                    state = STATE_MENU_IN;
                }
                break;

			case STATE_MENU_IN:
				accMenuTime += lastFrameTime;
				if (accMenuTime < 256) {
					animateLeftDoor(g, accMenuTime);
					animateRightDoor(g, accMenuTime);
					animateBottomDisplay(g, accMenuTime);
					animateBottomMenuIcons(g, accMenuTime, bottomPrev, bottomNext);
					animateTopBorder(g, accMenuTime);
				} else {
					// redraw, init next state
					animateLeftDoor(g, 256);
					animateRightDoor(g, 256);
					animateBottomDisplay(g, 256);
					animateBottomMenuIcons(g, 256, bottomPrev, bottomNext);
					animateTopBorder(g, 256);
					accMenuTime = 0;
					state = STATE_MENU_DOOR_OUT;
				}
				break;

			case STATE_MENU_DOOR_OUT:
				accMenuTime += lastFrameTime;
				if (accMenuTime < 96) {
					// nop
				} else if (accMenuTime < 224) {
					animateMenuAsDoor(g, 256);
					animateRightDoor(g, (224-accMenuTime)<<1);
				} else {
					// init next state
					accMenuTime = 0;
					state = STATE_MENU;
				}
				break;

			case STATE_MENU_DOOR_IN:
				accMenuTime += lastFrameTime;
				if (accMenuTime < 0) {
					accMenuTime = 0;
                    disposeMenu(false);
                    System.gc();
					initLevel();
					state = STATE_MENU_TO_GAME;
				} else if (accMenuTime < 128) {
					animateRightDoor(g, accMenuTime<<1);
				} else {
					accMenuTime = Integer.MIN_VALUE;
					animateRightDoor(g, 256);
				}
				break;

			case STATE_MENU_TO_GAME:
				paintLevel(g);
				accMenuTime += lastFrameTime;
				if (accMenuTime < 256) {
					animateLeftDoor(g, 256-accMenuTime);
					animateRightDoor(g, 256-accMenuTime);
					animateBottomDisplay(g, 256);
					animateBottomMenuIcons(g, 256-accMenuTime, bottomPrev, bottomNext);
				} else {
					// init next state
					animateBottomDisplay(g, 256);
					accMenuTime = 0;
					state = STATE_GAME_TOP_IN;
					disposeMenu(true);
				}
				break;
                                
			case STATE_MENU_CHAPTER_OPEN:
				paintChapter(g);
				accMenuTime += lastFrameTime;
				if (accMenuTime<256) {
                    animateLeftDoor(g, 256-accMenuTime);
                    animateRightDoor(g, accMenuTime);
                                
				} else {
                    animateRightDoor(g, 256);
                    state = STATE_MENU_CHAPTER;
                    chapterScrollDelta = 0;
				}
				break;
                                
                                
			case STATE_MENU_CHAPTER:
				int dx = lastFrameTime*2;
				if (chapterScrollDelta<0) {
					if (chapterBigImageX>0&&chapterBigImageX<=dx) {
						chapterBigImageX=0;
						chapterScrollDelta=0;
					} else {
						chapterBigImageX -= dx;
						if (chapterBigImageX<=-256) {
							chapterBigImageX = 0;
							chapterScrollDelta = 0;
							selectedChapter++;
						}
					}
				} else if (chapterScrollDelta>0) {
					if (chapterBigImageX<0&&chapterBigImageX>=-dx) {
						chapterBigImageX=0;
						chapterScrollDelta=0;
					} else {
						chapterBigImageX += dx;
						if (chapterBigImageX>=256) {
							chapterBigImageX = 0;
							chapterScrollDelta = 0;
							selectedChapter--;
						}
					}
				}
				paintChapter(g);
				break;
                
			case STATE_MENU_CHAPTER_CLOSE:
				paintChapter(g);
				accMenuTime -= lastFrameTime;
				if (accMenuTime>0) {
					animateLeftDoor(g, 256-accMenuTime);
					//animateRightDoor(g, 256-accMenuTime);
				} else {
					animateLeftDoor(g, 256);
					if (storyBeforeLevel[levelNum] >= 0) {
					    initStory(storyBeforeLevel[levelNum]);
					    accMenuTime=0;
					    state = STATE_MENU_TO_STORY;
					} else {
                        accMenuTime = Integer.MIN_VALUE;
					    state = STATE_MENU_DOOR_IN;
					}
				}
				break;

			case STATE_GAME_TO_MENU_PAR:
				accMenuTime += lastFrameTime;
                if (accMenuTime < 0) {
                    disposeLevel();
                    System.gc();
                    playMidi("/menu.mid");
                    loadMenu(true);
                    accMenuTime = 0;

                    resetLevel();	// ez mindig kell, ha visszatertunk a menube a jatekbol
                    if (prevState == STATE_GAME_LEVEL_END) {
                       if (maxLevelNum < levelNum+1) maxLevelNum = levelNum+1;
                    }
                    saveGame();
                    prevState = state;
					state = nextState;  //STATE_MENU_DOOR_OUT;

                } else if (accMenuTime < 256) {
    				paintLevel(g);
					animateLeftDoor(g, accMenuTime);
					animateRightDoor(g, accMenuTime);
					animateBottomDisplay(g, 256);
					animateBottomMenuIcons(g, accMenuTime, bottomPrev, bottomNext);

                } else {
					// redraw, init next state
					animateLeftDoor(g, 256);
					animateRightDoor(g, 256);
					animateBottomDisplay(g, 256);
					animateBottomMenuIcons(g, 256, bottomPrev, bottomNext);
                    accMenuTime = Integer.MIN_VALUE;
				}
				break;

            case STATE_GAME_TO_SCORES:
                if (accMenuTime <= 0) {
                    resetLevel(); // gameover miatt
                    loadStoryImages();
                }
                paintStoryBackground(g, lastFrameTime);
				paintHighScore(g, accMenuTime);
				accMenuTime += lastFrameTime;
				if (accMenuTime < 256) {
					animateLeftDoor(g, 256-accMenuTime);
					animateRightDoor(g, 256-accMenuTime);
				} else {
					accMenuTime = 0;
					state = STATE_SCORES;
				}
				break;

            case STATE_MENU_TO_SCORES:
                if (accMenuTime <= 0) {
                    loadStoryImages();
                }
                paintStoryBackground(g, lastFrameTime);
				paintHighScore(g, accMenuTime);
				accMenuTime += lastFrameTime;
				if (accMenuTime < 256) {
					animateLeftDoor(g, 256-accMenuTime);
					animateMenuAsDoor(g, 256-accMenuTime);
				} else {
					accMenuTime = 0;
					state = STATE_SCORES;
				}
				break;

            case STATE_SCORES:
                paintStoryBackground(g, lastFrameTime);
				paintHighScore(g, accMenuTime);
                accMenuTime += lastFrameTime;
                break;

			case STATE_SCORES_TO_MENU:
                paintStoryBackground(g, lastFrameTime);
				paintHighScore(g, accMenuTime);
				accMenuTime += lastFrameTime;
				if (accMenuTime < 256) {
					animateLeftDoor(g, accMenuTime);
					animateMenuAsDoor(g, accMenuTime);
				} else {
					animateLeftDoor(g, 256);
					animateMenuAsDoor(g, 256);
                    disposeStoryImages();
					accMenuTime = 0;
                    editingHighScore = -1;
					state = STATE_MENU;
				}
				break;

            case STATE_MENU_UPGRADE_OPEN:
				paintUpgrade(g);
				accMenuTime += lastFrameTime;
				if (accMenuTime < 256) {
					animateLeftDoor(g, 256-accMenuTime);
				} else {
					accMenuTime = 0;
					state = STATE_MENU_UPGRADE;
				}
				break;

			case STATE_MENU_UPGRADE_CLOSE:
				paintUpgrade(g);
				accMenuTime += lastFrameTime;
				if (accMenuTime < 256) {
					animateLeftDoor(g, accMenuTime);
				} else {
					animateLeftDoor(g, 256);
					accMenuTime = 0;
					state = STATE_MENU;
				}
				break;

			case STATE_MENU_UPGRADE:
				paintMenu(g);
				paintUpgrade(g);
				break;

			case STATE_MENU:
                if (accMenuTime == 0) {
                    if (loadGamePossible()) {
                        menu[6] = 6;
                    } else {
                        menu[6] = -1;
                    }
                } else {
                    accMenuTime = 1;
                }
				paintMenu(g);
				break;

			case STATE_GAME_TOP_IN:
				paintLevel(g);
				accMenuTime += lastFrameTime;
				if (accMenuTime < 128) {
					animateTopLives(g, accMenuTime<<1);
					animateTopScore(g, accMenuTime<<1);
					animateBottomDisplay(g, 256);
					animateBottomMenuIcons(g, accMenuTime<<1, null, bottomHP);
				} else {
					animateTopLives(g, 256);
					animateTopScore(g, 256);
					animateBottomDisplay(g, 256);
					animateBottomMenuIcons(g, 256, null, bottomHP);
					accMenuTime = 0;
					state = STATE_GAME;
				}
				break;

			case STATE_GAME_TOP_OUT:
				paintLevel(g);
				accMenuTime += lastFrameTime;
				if (accMenuTime < 0) {
                    accMenuTime = 0;
                    loadMenu(false);
					state = STATE_GAME_TO_MENU_PAR;     // a nextState megy tovabb
                } else if (accMenuTime < 128) {
                    int remTime = 256-(accMenuTime<<1);
					animateTopBorder(g, 256);
					animateTopLives(g, remTime);
					animateTopScore(g, remTime);
					animateBottomDisplay(g, 256);
					animateBottomMenuIcons(g, remTime, null, bottomHP);
				} else {
					animateTopBorder(g, 256);
					animateTopLives(g, 0);
					animateTopScore(g, 0);
					animateBottomDisplay(g, 256);
					animateBottomMenuIcons(g, 0, null, bottomHP);
					accMenuTime = Integer.MIN_VALUE;
				}
				break;

            case STATE_GAME:
				paintLevel(g);

				// top
				if (prevPlayerLives != playerLives) {   // LO-RES-ben mindig ujra kell rajzolni!!!
                    g.setClip(0, 0, scrX, scrY);
                    g.drawImage(topLives, scrX-topLives.getWidth(), 0, Graphics.TOP|Graphics.LEFT);
                    writeNumLeft(g, scrX-top3NumXOffset, top3NumYOffset, playerLives);
                    prevPlayerLives = playerLives;
                }

				if (prevScore != score) {   // LO-RES-ben mindig ujra kell rajzolni!!!
                    g.setClip(0, 0, scrX, scrY);
                    g.drawImage(topScore, 0, 0, Graphics.TOP|Graphics.LEFT);
        			writeNumRight(g, top1Width-top1NumXOffset, top1NumYOffset, score);
					prevScore = score;
                }

				// hp
				if (shipPrevHP != shipHP) {
    				g.setClip(0, 0, scrX, scrY);
    				g.drawImage(bottomRightDisplay, scrX-bottom3Width, scrY-bottomHeight, Graphics.TOP|Graphics.LEFT);
					shipPrevHP = shipHP;
					int hpLen = (shipHP*((button3Width<<8)/maxShipHP)+128)>>8;
                    if (hpLen > 0) {
                        int xPos = scrX-bottom3Width+bottom3ButtonXOffset;
                        int yPos = scrY-bottomHeight+bottomButtonYOffset;
                        g.setClip(xPos, yPos, hpLen, bottomHeight);
                        g.drawImage(bottomHP, xPos, yPos, Graphics.TOP|Graphics.LEFT);
                    }
				}

				// foellen hp
                if (mainEnemy != null && prevMainEnemyHP != mainEnemy.hp) {
    				g.setClip(0, 0, scrX, scrY);
            		g.drawImage(bottomLeftDisplay, 0, scrY-bottomHeight, Graphics.TOP|Graphics.LEFT);
					prevMainEnemyHP = mainEnemy.hp;
					int hpLen = (mainEnemy.hp*((button1Width<<8)/mainEnemy.entype.hp)+128)>>8;
                    if (hpLen > 0) {
                        g.setColor(255, 0, 0);
                        g.fillRect(bottom1ButtonXOffset, scrY-bottomHeight+bottomButtonYOffset, hpLen, buttonHeight);
                    }
                }
				break;
                
			case STATE_GAME_QUITQUESTION:
				writeStringCentered(g, scrX>>1, ptop+(pheight>>1)/*-fontHeight*/+4, messages[2+gameMessageIndex]);
				break;
                
			case STATE_MENU_NEWQUESTION:
				writeStringCentered(g, scrX>>1, ptop+(pheight>>1)/*-fontHeight*/+4, messages[3+gameMessageIndex]);
				break;

            case STATE_DISSOLVE_PAR:
				accMenuTime += lastFrameTime;
				if (accMenuTime < 256) {
					animateDissolve(g, accMenuTime);
				} else {
					animateDissolve(g, 256);
					dissolveBitfield = null;
					accMenuTime = 0;
					state = nextState;
				}
				break;

			case STATE_WEAPONINFO_IN:
				paintUpgrade(g);
				accMenuTime += lastFrameTime;
				if (accMenuTime < 128) {
					animateWeaponInfo(g, accMenuTime<<1);
					animateTopScore(g, accMenuTime<<1);
				} else {
					animateWeaponInfo(g, 256);
					animateTopScore(g, 256);
					accMenuTime = 0;
					state = STATE_MENU_UPGRADE;
				}
				break;

			case STATE_WEAPONINFO_OUT:
				paintUpgrade(g);
				accMenuTime += lastFrameTime;
				if (accMenuTime < 128) {
					animateWeaponInfo(g, 256-(accMenuTime<<1));
					animateTopBorder(g, 256);
					animateTopScore(g, 256-(accMenuTime<<1));
				} else {
					animateTopBorder(g, 256);
					animateWeaponInfo(g, 0);
					accMenuTime = 0;
					topScorePos = 0;
					state = STATE_MENU_UPGRADE;
				}
				break;

			case STATE_NOT_ENOUGH_MONEY:	// megvillogtatja a score-t
				accMenuTime += lastFrameTime;
				if (accMenuTime < 192) {
    				topScorePos = 0;
        			paintUpgrade(g);
					animateTopScore(g, 256);
					if ((accMenuTime&32) == 0) writeNumRight(g, top1Width-top1NumXOffset, top1NumYOffset, score);
				} else {
					topScorePos = top1Height;
					accMenuTime = 0;
					state = STATE_MENU_UPGRADE;
				}
				break;

            case STATE_GAME_TO_STORY:
                if (accMenuTime <= 0) loadStoryImages();
                paintStoryBackground(g, lastFrameTime);
				paintStory(g, -1);
				accMenuTime += lastFrameTime;
				if (accMenuTime < 256) {
					animateLeftDoor(g, 256-accMenuTime);
					animateRightDoor(g, 256-accMenuTime);
				} else {
					state = STATE_STORY;
				}
				break;

            case STATE_MENU_TO_STORY:
                if (accMenuTime <= 0) loadStoryImages();
                paintStoryBackground(g, lastFrameTime);
				paintStory(g, -1);
				accMenuTime += lastFrameTime;
				if (accMenuTime < 256) {
					animateLeftDoor(g, 256-accMenuTime);
					animateRightDoor(g, 256-accMenuTime);
				} else {
					state = STATE_STORY;
				}
				break;

            case STATE_STORY:
                paintStoryBackground(g, lastFrameTime);
				paintStory(g, accMenuTime);
                accMenuTime += lastFrameTime;
                break;

            case STATE_STORY_TO_MENU:
                paintStoryBackground(g, lastFrameTime);
				paintStory(g, -1);
				accMenuTime += lastFrameTime;
				if (accMenuTime < 256) {
					animateLeftDoor(g, accMenuTime);
					animateMenuAsDoor(g, accMenuTime);
				} else {
					animateLeftDoor(g, 256);
					animateMenuAsDoor(g, 256);
                    disposeStoryImages();
					accMenuTime = 0;
					state = STATE_MENU;
				}
				break;

            case STATE_STORY_TO_SCORES:
                paintStoryBackground(g, lastFrameTime);
				paintStory(g, -1);
				accMenuTime += lastFrameTime;
				if (accMenuTime < 256) {
					animateLeftDoor(g, accMenuTime);
					animateRightDoor(g, accMenuTime);
				} else {
					animateLeftDoor(g, 256);
					animateRightDoor(g, 256);
                    disposeStoryImages();
					accMenuTime = 0;
					state = STATE_SCORES;
				}
				break;

            case STATE_STORY_TO_GAME:
				if (accMenuTime < 0) {
                    disposeStoryImages();
                    initLevel();
                    accMenuTime = 0;
					state = STATE_MENU_TO_GAME;
                } else {
                    paintStoryBackground(g, lastFrameTime);
        			paintStory(g, -1);
            		accMenuTime += lastFrameTime;
                    if (accMenuTime < 256) {
                        animateLeftDoor(g, accMenuTime);
                        animateRightDoor(g, accMenuTime);
                    } else {
    					animateLeftDoor(g, 256);
    					animateRightDoor(g, 256);
    					accMenuTime = Integer.MIN_VALUE;
    				}
                }
				break;

            case STATE_GAME_LEVEL_END:
                shouldDrawShip = false;
                paintLevel(g);
				accMenuTime += lastFrameTime;
                if (accMenuTime < 0) {
                    loadMenu(false);
                    playMidi("/menu.mid");

                    if (storyAfterLevel[levelNum] >= 0) {
                        initStory(storyAfterLevel[levelNum]);
                        nextState = STATE_GAME_TO_STORY;
                    } else {
    					nextState = STATE_MENU_DOOR_OUT;
                    }
                    accMenuTime = 0;
                    prevState = state;
                    state = STATE_GAME_TO_MENU_PAR;

                } else if (accMenuTime < 128) {
                    
                    // ship
            		int actShipX = ((fShipX-fenshift+128)>>8)+pleft;
                    int actShipShift = (accMenuTime*accMenuTime*pheight*4)>>8;
                    int actShipY = ((fShipY-actShipShift+128)>>8)+ptop;

                    // extragun
                    if (shipWeaponExtra >= 0) {
                        int extraGunX = (fExtraGunX-fenshift+128)>>8;
                        int extraGunY = ((fExtraGunY-actShipShift)>>8)+ptop;
                        g.setClip(0, ptop, scrX, pheight);
                        g.drawImage(bulletImages[15+shipWeaponExtra], extraGunX, extraGunY, Graphics.TOP|Graphics.LEFT);
                    }

                    g.setClip(actShipX, actShipY, shipWidth, shipHeight);
                    g.drawImage(shipImage, actShipX-(((shipSlideTime+16)>>5)*shipWidth), actShipY, Graphics.TOP|Graphics.LEFT);

                    // kezelofelulet
                    int remTime = 256-(accMenuTime<<1);
					animateTopLives(g, remTime);
					animateTopScore(g, remTime);
					animateBottomDisplay(g, 256);
					animateBottomMenuIcons(g, remTime, null, bottomHP);
					animateTopBorder(g, 256);

				} else {
					animateTopLives(g, 0);
					animateTopScore(g, 0);
					animateBottomDisplay(g, 256);
					animateTopBorder(g, 256);
					accMenuTime = Integer.MIN_VALUE;
				}
				break;

            case STATE_EXIT:
    			myMIDlet.notifyDestroyed();
                break;

		}

		//System.out.println(" endstate: "+state+"; done.");
        //System.out.flush();
	}

/**********************************************************************************
/* PAINTLEVEL
/**********************************************************************************/
	static int x1max, x2min, y1max, y2min;
	final static boolean collide(int ax1, int ay1, int ax2, int ay2, int bx1, int by1, int bx2, int by2) {
        //System.out.println("COLLIDE; AX1: "+ax1+", AY1: "+ay1+", AX2: "+ax2+", AY2: "+ay2);
        //System.out.println("=============> BX1: "+bx1+", BY1: "+by1+", BX2: "+bx2+", BY2: "+by2);
		x1max = (ax1 < bx1)?bx1:ax1;
		x2min = (ax2 > bx2)?bx2:ax2;

		if (x1max < x2min) {
			y1max = (ay1 < by1)?by1:ay1;
			y2min = (ay2 > by2)?by2:ay2;

			if (y1max < y2min) {
				return true;
			}
		}
		return false;
	}

	final static void createParticle(int fx, int fy) {
        for (int i = 0; i < particlefTime.length; i++) {
            if (particlefTime[i] <= 0) {
                particlefTime[i] = 80;
                particlefXPos[i] = fx;
                particlefYPos[i] = fy;
                particleAngleSin[i] = -65536;
                particleAngleCos[i] = 0;
                particleSpeed[i] = 20;
                particleAnimPos[i] = 0;
                break;
            }
        }
	}

    final static void createParticles(int fx, int fy, int damage, int timeAnd, int speedAnd, int speedOffset) {
		int num = 2+(damage>>1);
		for (int j = 0; j < num; j++) {
			for (int i = 0; i < particlefTime.length; i++) {
				if (particlefTime[i] <= 0) {
					particlefTime[i] = Math.abs(random.nextInt())&timeAnd;
                    int angle = Math.abs(random.nextInt())%360;
                    particleAngleSin[i] = Stuff.sin(angle);
                    particleAngleCos[i] = Stuff.cos(angle);
					particlefXPos[i] = fx;
					particlefYPos[i] = fy;
                    particleSpeed[i] = (Math.abs(random.nextInt())&speedAnd)+speedOffset;
                    particleAnimPos[i] = 0;
                    break;
				}
			}
		}
	}

	static void paintChapter(Graphics g) {
		g.setClip(0, 24, leftDoorWidth, 159);
		int x = chapterBigImageX*leftDoorWidth>>8;
        int chShaded = selectedChapter <= maxLevelNum?0:1;
   		g.drawImage(chapterLabelImages[chShaded], x, 24, Graphics.TOP|Graphics.LEFT);

		int w = leftDoorWidth-x-74;
		if (w>0) {
			if (w>15)w=15;
			g.setClip(x+74, 30, w, 11);
			g.drawImage(chapterNumImages[chShaded], x+74-selectedChapter*15, 30, Graphics.TOP|Graphics.LEFT);
			g.setClip(0, 24, leftDoorWidth, 159);
		}

		g.drawImage(chapterBigImages[chapterBigImageIndices[selectedChapter]], x, 48, Graphics.TOP|Graphics.LEFT);
		if (x>0) {
            chShaded = selectedChapter-1 <= maxLevelNum?0:1;
			g.drawImage(chapterLabelImages[chShaded], x-leftDoorWidth, 24, Graphics.TOP|Graphics.LEFT);
			w = leftDoorWidth-x-74+leftDoorWidth;
			if (w>0) {
				if (w>15)w=15;
				g.setClip(x+74-leftDoorWidth, 30, w, 11);
				g.drawImage(chapterNumImages[chShaded], x+74-(selectedChapter-1)*15-leftDoorWidth, 30, Graphics.TOP|Graphics.LEFT);
				g.setClip(0, 24, leftDoorWidth, 159);
			}
			g.drawImage(chapterBigImages[chapterBigImageIndices[selectedChapter-1]], x-leftDoorWidth, 48, Graphics.TOP|Graphics.LEFT);
		} else if (x<0) {
            chShaded = selectedChapter+1 <= maxLevelNum?0:1;
			g.drawImage(chapterLabelImages[chShaded], x+leftDoorWidth, 24, Graphics.TOP|Graphics.LEFT);
			w = leftDoorWidth-x-74-leftDoorWidth;
			if (w>0) {
				if (w>15)w=15;
				g.setClip(x+74+leftDoorWidth, 30, w, 11);
				g.drawImage(chapterNumImages[chShaded], x+74-(selectedChapter+1)*15+leftDoorWidth, 30, Graphics.TOP|Graphics.LEFT);
				g.setClip(0, 24, leftDoorWidth, 159);
			}
			g.drawImage(chapterBigImages[chapterBigImageIndices[selectedChapter+1]], x+leftDoorWidth, 48, Graphics.TOP|Graphics.LEFT);
		}
	}


    static void paintLevel(Graphics g) {
		// layers
		for (int i = 0; i < bgLayers.length; i++)
			bgLayers[i].draw(g);

		// bulletmozgas
		int actLastBullet = 0;
		for (int i = 0; i < bulletLast; i++) {
			Bullet b = bullets[i];
			if (b != null) {
				if (b.type == 13) { // celkoveto - get closest enemy
					int mind = Integer.MAX_VALUE;
					int mindx = 0, mindy = 0;
					for (int j = enBegin; j < enemies.length; j++) {
						if (enemies[j] != null) {
							Enemy en = enemies[j];
                            if (en.explosionTime >= 0) continue;        // ignore

                            if (en.pathPoint >= 0) {
								int dx = (en.fxAct+(fenWidth[en.type]>>1)) - b.fxAct;		// optme
								int dy = (en.fyAct+(fenHeight[en.type]>>1)) - b.fyAct;
								int actd = dx*dx+dy*dy;

								if (actd < mind) {
									mind = actd;
									mindx = dx;
									mindy = dy;
								}
							} else {
								break;
							}
						}
					}
					if (mind < Integer.MAX_VALUE) {	// mozgatjuk
						mind = Stuff.sqrt(mind);
						b.seek(mindx, mindy, mind, lastFrameTime);
					} else {
						b.move(lastFrameTime);		// nincs cel, folytassa a mozgast
					}

				} else {
					b.move(lastFrameTime);
                    if (b.type == 24 && b.spawnParticle > 50) { // raketa
                        createParticle(b.fxAct, b.fyAct+rocketParticleYOffset);
                        b.spawnParticle = 0;
                    }
				}
				actLastBullet = i;
			}
		}
		bulletLast = actLastBullet+1;
		//System.out.println("lastbullet: "+bulletLast);

		enbgpos = (int)(((accFrameTime+fAccBGTime)*fEnemyBGSpeed)>>16)+pheight;
		int fShipLeft = fShipX, fShipRight = fShipLeft+fShipWidth, fShipTop = fShipY, fShipBottom = fShipY+fShipHeight;
		int fShipMiddleX = fShipLeft+(fShipWidth>>1), fShipMiddleY = fShipY+(fShipHeight>>1);
        int actvill = (accFrameTime/170)%3;

        //System.out.println("Ship: "+(fShipX>>8)+", "+(fShipY>>8));

        // stat. main enemy
        if (mainEnemy != null && mainEnemy.type == 22 && scrollLevel) {
            if (mainEnemy.bgPos <= enbgpos) scrollLevel = false;
        }

		// bullet-krealas
		if (shipDestroyTime < 0 && shouldDrawShip) {
            if (shipWeaponFront >= 0 && shipWeaponFront < 12) {
                shipFrontFired += lastFrameTime;
                while (shipFrontFired > weaponRate[shipWeaponFront]) {
                    shipFrontFired -= weaponRate[shipWeaponFront];
                    fireWeapon(false, fShipMiddleX, fShipY, shipWeaponFront);
                }
            }

            if (shipWeaponBack >= 12 && shipWeaponBack < 16) {
                shipBackFired += lastFrameTime;
                while (shipBackFired > weaponRate[shipWeaponBack]) {
                    shipBackFired -= weaponRate[shipWeaponBack];
                    fireWeapon(false, fShipMiddleX, fShipY-(4<<8), shipWeaponBack);
                }
            }

            if (shipWeaponShield >= 0) {
                int ad = (lastFrameTime*90+128)>>8;
                for (int i = 0; i <= shipWeaponShield; i++) {
                    //System.out.println("bullet "+i+": "+shieldBulletIndex[i]);
                    Bullet b = bullets[shieldBulletIndex[i]];
                    b.angle += ad;
                    while (b.angle >= 360) b.angle -= 360;
                    b.fxAct = fShipMiddleX+((Stuff.cos(b.angle)*fShipWidth)>>16)-(fBulletWidth[14]>>1);		// optme: ezt beirni konstansra
                    b.fyAct = fShipMiddleY+((Stuff.sin(b.angle)*fShipHeight)>>16)-(fBulletHeight[14]>>1);
                }
            }

            if (shipWeaponExtra >= 0) {
                int actBullet = 15+shipWeaponExtra;
                int actWeapon = shipWeaponExtra*3;

                // mozgatas a ship fele
                int actD = extraGunSpeed*lastFrameTime;
                int diffX = fShipX;
                if (shipWeaponExtraLeft) {
                    diffX -= fBulletWidth[actBullet];
                } else {
                    diffX += fShipWidth;
                }
                diffX -= fExtraGunX;
                int diffY = fShipY+fShipHeight-fBulletHeight[actBullet]-extraGunVerticalTilt[shipWeaponExtra] - fExtraGunY;
                int targetD = Stuff.sqrt(diffX*diffX+diffY*diffY);
                if (targetD <= actD) {
                    fExtraGunX += diffX;
                    fExtraGunY += diffY;
                } else {
                    int ratio = (actD<<8)/targetD;
                    fExtraGunX += (diffX*ratio+128)>>8;
                    fExtraGunY += (diffY*ratio+128)>>8;

                }

                shipExtraFired += lastFrameTime;
                while (shipExtraFired > weaponRate[actWeapon]) {
    				shipExtraFired -= weaponRate[actWeapon];
    				fireWeapon(false, fExtraGunX+(fBulletWidth[actBullet]>>1), fExtraGunY, actWeapon);
    			}
            }
		}

		// particle-mozgatas
		for (int i = 0; i < particlefTime.length; i++) {
			if (particlefTime[i] > 0) {
				particlefTime[i] -= lastFrameTime;
                int d = lastFrameTime*particleSpeed[i];
				particlefXPos[i] += (particleAngleSin[i]*d)>>16;
				particlefYPos[i] += (particleAngleCos[i]*d)>>16;
			}
		}

		// enemy
		for (int i = enBegin; i < enemies.length; i++) {
			if (enemies[i] == null) {
				if (i == enBegin) enBegin++;
				//System.out.println("enBegin: "+enBegin);
				continue;
			}

			Enemy en = enemies[i];

			// actor
			if (en.type >= 64) {
				if (en.bgPos <= enbgpos) {	// actor teendo
					switch (en.type) {
						case 64:								// message
							messageID = en.actorParams[0];
							messageTime = en.actorParams[1]*256;
							break;

						case 65:								// checkpoint
                            if (accFrameTime > 768) {
                                messageID = 0+gameMessageIndex;
                                messageTime = 3*256;
                                fAccBGTime = accFrameTime;
                                accFrameTime = 0;
                                saveGame(); // rogton el is mentjuk
                            }
							break;

						case 66:								// sebvaltas - kiiktatva
							/*fAccBGPos = accFrameTime*fEnemyBGSpeed;
							fMaxBGSpeed = 0;
							for (int j = 0; j < bgLayers.length; j++) {
								fEnemyBGSpeed = en.actorParams[j<<1]+(en.actorParams[(j<<1)+1]<<8);
								if (fEnemyBGSpeed > fMaxBGSpeed) fMaxBGSpeed = fEnemyBGSpeed;
								bgLayers[j].setSpeed(fEnemyBGSpeed);
							}

							maxvscrdelta = (maxBGShift<<16)/fMaxBGSpeed;
							fMaxEnShift = maxBGShift*((fEnemyBGSpeed<<8)/fMaxBGSpeed);
							penwidth = pwidth+((fMaxEnShift)>>7);
                            fpenwidth = penwidth<<8;
							penmiddle = penwidth>>1;

							int oldLimit = fShipLimitX;
							fShipLimitX = (penwidth-shipWidth)<<8;
							fShipX = (fShipX*((fShipLimitX<<8)/oldLimit)+128)>>8;
							movePlayer(1, 1, 0);	// init shift values
							accFrameTime = 0;*/
							break;

						case 67:								// end level
                            // EVENT: palya vege // FIXME! gyanus
                            saveGame();
                            state = STATE_GAME_LEVEL_END;
							break;
					}
					enemies[i] = null;
				}
				continue;
			}

			if (en.pathPoint >= 0) {
				if (en.explosionTime < 0) {
					en.move(lastFrameTime);

				} else {
					if (en.p == null) {				// stationary
						en.move(lastFrameTime);
						if (en.type < 16) {
                            en.fyAct += (fenHeight[en.type] - fExplSize)>>1;	// optme + opt stat enemy
                        }
                    }

                    en.explosionTime += lastFrameTime;

					if (en.type < 16) {
                        if (en.explosionTime >= 192) {
                            enemies[i] = null;
                            continue;
                        }
					} else if (en.type < 20) {

                        if (en.fwait == 0) {
                            if (en.explosionTime < 192) {       // explosion size korrigalas
                                en.fyAct += (fenHeight[en.type] - fExplSize)>>1;
                            } else {
                                en.fxAct -= (fenWidth[en.type] - fExplSize)>>1;
                                //en.fyAct += (fenHeight[en.type] - fExplSize)>>1;
                                en.fwait = 1;
                            }
                        }

                    } else {
                        if (en.explosionTime >= 1280) {
                            enemies[i] = null;

                            // EVENT: palya vege
                            state = STATE_GAME_LEVEL_END;
                            continue;
                        }
                    }
				}

			} else if (en.bgPos <= enbgpos+enHeight[en.type]) {	// aktival
					en.activate();
                    if (en.type >= 20) {    // foellen
                        mainEnemy = en;
                        prevMainEnemyHP = -1;
                        meDestroyTime = meDestroyX = meDestroyY = null;
                        mainEnemyFront = 0;
                    }

			} else {
				break;
			}

			// real -> virtual poziciok
			int fxPos = en.fxAct;
			int fyPos = en.fyAct;
			int fxPos2, fyPos2;

            //System.out.println("en: "+i+", X: "+(fxPos>>8)+", Y: "+(fyPos>>8)+", explTime: "+en.explosionTime);

			if (en.explosionTime < 0 && en.ghostTime < 0 && (en.type != 19 || actvill != en.fwait)) {
				fxPos2 = fxPos + fenWidth[en.type];
				fyPos2 = fyPos + fenHeight[en.type];

                if (en.type == 19) fyPos2 -= 8<<8;

                // enemy shot
                if (en.lastShot >= 0) {
                    en.lastShot += lastFrameTime;
                    while (en.lastShot > en.bulletFreq) {
                        en.lastShot -= en.bulletFreq;
                        fireWeapon(true, fxPos+(fenWidth[en.type]>>1), fyPos+(fenHeight[en.type]>>1) /*fyPos2-(5<<8)*/, en.entype.bulletType);
                    }
                }

				// utkozes - enemy vs. ship
				if (shipBlinkingTime<0 && shipDestroyTime < 0 && collide(fxPos, fyPos, fxPos2, fyPos2, fShipLeft, fShipTop, fShipRight, fShipBottom)) {
					int damage = Math.min(shipHP, en.hp);
                    shipHP -= damage;
                    en.hp -= damage;

                    shipFlashTime = 127;
                    shipFlashType = FLASH_RED;

                    int enWidthHalf = fenWidth[en.type]>>1;
                    int enHeightHalf = fenHeight[en.type]>>1;

                    if (en.hp <= 0) {
                        en.explosionTime = 0;

                        // explosion size korrigalas
                        if (en.type < 20) {
                            int dx = enWidthHalf-(fExplSize>>1);
                            int dy = enHeightHalf-(fExplSize>>1);
                            en.fxAct += dx;
                            en.fyAct += dy;
                            fxPos += dx;
                            fyPos += dy;
                            fxPos2 = fxPos+fExplSize;
                            fyPos2 = fyPos+fExplSize;
                        }
                    }

                    if (shipHP <= 0) {
                        playerLives--;
                        shipDestroyTime = 0;
                        fShipX += (shipWidth-explSize)<<7;
                        fShipY += (shipHeight-explSize)<<7;
                    } else {
                        score += en.entype.point;
                        actScore += en.entype.point;

                        if (actScore >= levelNumHPScore) {   // kirakjuk a hp-bulletet
                            actScore -= levelNumHPScore;
                            int nextInd = 0;
                            while (bullets[nextInd] != null) nextInd++;
                            bullets[nextInd] = new Bullet(true, en.fxAct+(enWidthHalf-(8<<7)), en.fyAct+(enHeightHalf-(8<<7)), 270, fEnemyBGSpeed>>8, 34, 0);
                        }
                    }
                    playSound(SOUND_EXPLOSION);

				} else if (en.type != 19) {     // nem villam
					// utkozes - enemy vs. bullet
					for (int j = 0; j < bulletLast; j++) {
						Bullet b = bullets[j];

						if (b != null && !b.enemy) {
							int fbulletLeft = b.fxAct;
							int fbulletRight = fbulletLeft + fBulletWidth[b.type];
							int fbulletTop = b.fyAct;
							int fbulletBottom = fbulletTop + fBulletHeight[b.type];

							if (collide(fxPos, fyPos, fxPos2, fyPos2, fbulletLeft, fbulletTop, fbulletRight, fbulletBottom)) {
								en.hp -= b.damage;
								if (b.type==9||b.type==10||b.type==11) {
									if (en.hp<0) {
										b.damage = -en.hp;
									} else {
										bullets[j]=null;
									}
								} else if (b.type != 14) bullets[j] = null;	// shield

								if (en.hp <= 0) {
									en.explosionTime = 0;
									score += en.entype.point;
									actScore += en.entype.point;

									int enWidthHalf = fenWidth[en.type]>>1;
									int enHeightHalf = fenHeight[en.type]>>1;

									if (en.p != null && actScore >= levelNumHPScore) {   // kirakjuk a hp-bulletet
										actScore -= levelNumHPScore;
										int nextInd = 0;
										while (bullets[nextInd] != null) nextInd++;
										bullets[nextInd] = new Bullet(true, en.fxAct+(enWidthHalf-(8<<7)), en.fyAct+(enHeightHalf-(8<<7)), 270, fEnemyBGSpeed>>8, 34, 0);
									}

									playSound(SOUND_EXPLOSION);

									// explosion size korrigalas
									if (en.type < 20) {
										int dx = enWidthHalf-(fExplSize>>1);
										int dy = enHeightHalf-(fExplSize>>1);
										//System.out.println("dx: "+dx+", dy: "+dy);
										en.fxAct += dx;
										en.fyAct += dy;
										fxPos += dx;
										fyPos += dy;
										fxPos2 = fxPos+fExplSize;
										fyPos2 = fyPos+fExplSize;
									}

									createParticles((x1max+x2min)>>1, (y1max+y2min)>>1, b.damage, 255, 31, 20);
									// ne folytassa az utkozest
									break;

								} else {	// villanas + particle
									if (en.type < 16) en.flashTime = 32;
									createParticles((x1max+x2min)>>1, (y1max+y2min)>>1, b.damage, 127, 63, 20);
								}
							}
						}
					}
				}

			} else {
				fxPos2 = fxPos+fExplSize;
				fyPos2 = fyPos+fExplSize;
			}

			// ghost
			if (en.ghostTime >= 0 && en.explosionTime < 0) {
				int gxPos = (en.fxGhost*penwidth-fenshift+128)>>8;
				int gyPos = ((en.fyGhost*pheight+128)>>8)+ptop;

				//System.out.println("Draw: "+gxPos+", "+gyPos);
				if (gxPos < pright && gyPos < pbottom && gxPos+enWidth[10] > pleft && gyPos+enHeight[10] > ptop) {
					g.setClip(gxPos, Math.max(gyPos, ptop), enWidth[10], Math.min(enHeight[10], pbottom-gyPos));
					g.drawImage(enemyImages[10], gxPos-((4-(en.ghostTime>>5))*enWidth[10]), gyPos, Graphics.TOP|Graphics.LEFT);
				}
			}

			// fastenemy
			if (en.type == 9 && en.explosionTime < 0) {
				for (int j = 2; j >= 0; j--) {
					int sxPos = (en.shadows[j].fxAct - fenshift+128)>>8;
					int syPos = ((en.shadows[j].fyAct+128)>>8) + ptop;

					if (sxPos < pright && syPos < pbottom && sxPos+enWidth[9] > pleft && syPos+enHeight[9] > ptop) {
						g.setClip(sxPos, Math.max(syPos, ptop), enWidth[9], Math.min(enHeight[9], pbottom-syPos));
						g.drawImage(enemyImages[9], sxPos-((2+j)*enWidth[9]), syPos, Graphics.TOP|Graphics.LEFT);
					}
				}
			}

			int xPos = ((fxPos+128)>>8)-enshift;
			int yPos = ((fyPos+128)>>8)+ptop;
			int xPos2 = ((fxPos2+128)>>8)-enshift;
			int yPos2 = ((fyPos2+128)>>8)+ptop;

            // villam
            if (en.type == 19) {
                int actYPos = yPos-8;
                if (actYPos >= pbottom) {
                    enemies[i] = null;

                } else {

                    g.setClip(0, ptop, scrX, pheight);

                    if (actvill != en.fwait) {
                        // animate
                        byte[] actorParams = en.actorParams;
                        actorParams[0] += lastFrameTime;
                        if (actorParams[0] >= 16) {
                            actorParams[0] = 0;
                            actorParams[1] = (byte)(Math.abs(random.nextInt())%104);
                        }

                        // draw
                        for (int j = -(int)(actorParams[1]); j <= scrX; j += 104) {
                            g.drawImage(enemyImages[19], j, yPos, Graphics.TOP|Graphics.LEFT);
                        }
                    }

                    // tornyok
                    g.drawImage(enemyImages[18], 16-((fenshift+128)>>8), actYPos, Graphics.TOP|Graphics.LEFT);
                    g.drawImage(enemyImages[18], penwidth-48-((fenshift+128)>>8), actYPos, Graphics.TOP|Graphics.LEFT);
                }
                continue;
            }

			// rajta van-e a kepernyon
			if (xPos2 <= pleft || xPos >= pright || yPos2 <= ptop || yPos >= pbottom) {
				if (en.p != null) {
					if (en.pathPoint == en.p.length) {	// elerte a path veget es kifutott a kepbol
						enemies[i] = null;
					}
				} else {
					if (yPos >= pbottom) {		// stat ellen elerte a kep aljat
						enemies[i] = null;
                        //System.out.println("stat. en "+i+" removed");
					}
				}
				continue;
			}

			// explosion-kirajzolas
			if (en.explosionTime >= 0) {
				if (en.type < 18) {     // normal ellen
                    if (en.type >= 16) {
                        int actxPos, actyPos;

                        if (en.explosionTime < 192) {
                            int dx = (fenWidth[en.type] - fExplSize)>>1;
                            int dy = (fenHeight[en.type] - fExplSize)>>1;
                            actxPos = (fxPos-fenshift-dx)>>8;
                            actyPos = ((fyPos-dy+128)>>8)+ptop;
                        } else {
                            actxPos = xPos;
                            actyPos = yPos;
                        }

                        g.setClip(actxPos, Math.max(ptop, actyPos), enWidth[en.type], Math.min(enHeight[en.type], pbottom-actyPos));
                        g.drawImage(enemyImages[en.type], actxPos-enWidth[en.type], actyPos, Graphics.TOP|Graphics.LEFT);
                    }

                    g.setClip(xPos, Math.max(ptop, yPos), explSize, Math.min(explSize, pbottom-yPos));
                    g.drawImage(explosionImages[enExpl[en.type]], xPos-((en.explosionTime>>5)*explSize), yPos, Graphics.TOP|Graphics.LEFT);
                    continue;

                }
            }

			g.setClip(xPos, Math.max(ptop, yPos), enWidth[en.type], Math.min(enHeight[en.type], pbottom-yPos));
			if (en.flashTime > 0) {
				g.drawImage(enemyImages[en.type], xPos-enWidth[en.type], yPos, Graphics.TOP|Graphics.LEFT);
				en.flashTime -= lastFrameTime;
			} else if (en.ghostTime >= 0) {
				g.drawImage(enemyImages[en.type], xPos-((2+(en.ghostTime>>5))*enWidth[10]), yPos, Graphics.TOP|Graphics.LEFT);
			} else if (en.type < 20) {
				g.drawImage(enemyImages[en.type], xPos, yPos, Graphics.TOP|Graphics.LEFT);
            } else { // foellen

                if (en.explosionTime < 1024) {
    				g.drawImage(enemyImages[en.type], xPos, yPos, Graphics.TOP|Graphics.LEFT);
                }

                if (en.explosionTime >= 0) {
                    if (meDestroyTime == null) {
                        int len = 5;
                        if (en.type == 22) len=8;
                        meDestroyTime = new int[len];
                        meDestroyX = new int[len];
                        meDestroyY = new int[len];
                        for (int j = 0; j < len; j++) meDestroyTime[j] = Integer.MAX_VALUE;
                    }

                    boolean found = false;
                    for (int j = 0; j < meDestroyTime.length; j++) {
                        //System.out.print(""+j+".");
                        if (meDestroyTime[j] >= 192) {  // uj robbanas
                            if (found) continue;
                            found = true;
                            meDestroyTime[j] = 0;
                            int r = Math.abs(random.nextInt());
                            meDestroyX[j] = r%enWidth[en.type]-(explSize>>1);
                            meDestroyY[j] = r%enHeight[en.type]-(explSize>>1);
                            //System.out.print(" NEW");
                        }

                        int explX = xPos+meDestroyX[j];
                        int explY = yPos+meDestroyY[j];

                        //System.out.println(" Time: "+meDestroyTime[j]+" X: "+explX+", Y: "+explY+", misc: "+(explX-(meDestroyTime[j]&0xffffe0)));
                        g.setClip(explX, Math.max(ptop, explY), explSize, Math.min(explSize, pbottom-explY));
                        g.drawImage(explosionImages[(j&1)<<1], explX-((meDestroyTime[j]>>5)*explSize), explY, Graphics.TOP|Graphics.LEFT);

                        meDestroyTime[j] += lastFrameTime;
                    }
                }
            }
		}

		// utkozes - bullet vs. ship
        if (shipDestroyTime < 0) {
            for (int j = 0; j < bulletLast; j++) {
                Bullet b = bullets[j];

                if (b != null && b.enemy) {
                    int fbulletLeft = b.fxAct;
                    int fbulletRight = fbulletLeft + fBulletWidth[b.type];
                    int fbulletTop = b.fyAct;
                    int fbulletBottom = fbulletTop + fBulletHeight[b.type];

                    if (collide(fShipLeft, fShipTop, fShipRight, fShipBottom, fbulletLeft, fbulletTop, fbulletRight, fbulletBottom)) {
			boolean collided = true;
                        if (b.type >= 29 && b.type <= 33) {
                            shipHP = shipPrevHP-5;
                            if (shipFrostTime < b.damage) shipFrostTime = b.damage;
                            shipFlashTime = 127;
                            shipFlashType = FLASH_BLUE;

                        } else if (b.type == 34) {
                            shipHP = maxShipHP;
                            shipFlashTime = 127;
                            shipFlashType = FLASH_WHITE;
                            playSound(SOUND_UPGRADE);

                        } else if (shipBlinkingTime<0) {
                            shipHP -= b.damage;
                            shipFlashTime = 127;
                            shipFlashType = FLASH_RED;

                            if (shipHP < 0) {
                                playerLives--;
                                shipDestroyTime = 0;
                                fShipX += (shipWidth-explSize)<<7;
                                fShipY += (shipHeight-explSize)<<7;

                                playSound(SOUND_EXPLOSION);
                                // ne utkozzon tobb lovedekkel
                                break;
                            } else {
      							createParticles((x1max+x2min)>>1, (y1max+y2min)>>1, b.damage, 255, 31, 20);
                                playSound(SOUND_SHOT);
                            }
                        } else {
				collided = false;
			}
			if (collided) {
	                        bullets[j] = null;	// shield
			}
                    }
                }
            }
        } else {
            // cheat-vedelem - azonnal torlunk, ha meghott
            if (shipDestroyTime == 0 && playerLives < 0) deleteGame();

            if (shipDestroyTime >= 192 && state == STATE_GAME) {
                // EVENT: player halal
                if (playerLives >= 0) {
			shipBlinkingTime = 2*256;
			shipDestroyTime = -1-lastFrameTime;
			shipHP = maxShipHP;
			fShipX = fShipLimitX>>1;
			fShipY = fShipLimitY;
			/*
                    nextState = STATE_MENU_DOOR_OUT;

                    // continue -ra ugrunk
                    menuSelect = 1;
                    menuOffset = 5;
                    menuParent = 0;

                    // mentes
                    int deathLives = playerLives;
                    loadGame();
                    playerLives = deathLives;
                    saveGame();*/

                } else {    // game over
                    // EVENT: GAME OVER
                    nextState = STATE_GAME_TO_SCORES;
                    highScorePossible = true;
		    state = STATE_GAME_TOP_OUT;
                }

            }

            shipDestroyTime += lastFrameTime;
        }

		// bullet-kirakas
		for (int j = 0; j < bulletLast; j++) {
			Bullet b = bullets[j];
            if (b == null) continue;

            int actX = (b.fxAct-fenshift+128)>>8;
            int actY = ((b.fyAct+128)>>8)+ptop;
            int type = b.type;

            // palyan van-e meg
            if (b.fxAct <= -16<<8 || b.fxAct >= fpenwidth || actY+bulletHeight[type] <= ptop || actY >= pbottom) {
                if (b.type != 14) bullets[j] = null;	// shield
                //System.out.println("deleted "+i+", "+actX);
                continue;
            }

            g.setClip(actX, Math.max(actY, ptop), bulletWidth[type], Math.min(bulletHeight[type], pbottom-actY));
            g.drawImage(bulletImages[type], actX-(b.actFrame*bulletWidth[type]), actY, Graphics.TOP|Graphics.LEFT);
		}

		// ship-kirakas
		if (shouldDrawShip) {
            
            // extragun
            if (shipWeaponExtra >= 0) {
                int extraGunX = (fExtraGunX-fenshift+128)>>8;
                int extraGunY = (fExtraGunY>>8)+ptop;
                g.setClip(0, ptop, scrX, pheight);
                g.drawImage(bulletImages[15+shipWeaponExtra], extraGunX, extraGunY, Graphics.TOP|Graphics.LEFT);
            }

            int actShipX = ((fShipX-fenshift+128)>>8)+pleft;
            int actShipY = ((fShipY+128)>>8)+ptop;

            if (shipDestroyTime >= 0) {
                if (shipDestroyTime < 192) {
                    g.setClip(actShipX, Math.max(ptop, actShipY), explSize, Math.min(explSize, pbottom-actShipY));
                    g.drawImage(explosionImages[0], actShipX-((shipDestroyTime>>5)*explSize), actShipY, Graphics.TOP|Graphics.LEFT);
                }

            } else {

                if (shipBlinkingTime < 0 || (shipBlinkingTime & 32) == 32) {
                    g.setClip(actShipX, actShipY, shipWidth, shipHeight);
                    g.drawImage(shipImage, actShipX-(((shipSlideTime+16)>>5)*shipWidth), actShipY, Graphics.TOP|Graphics.LEFT);

// HI-RES BEGIN
                    int engineParticleX = actShipX+shipEngineParticleXOffset;
                    int engineParticleY = actShipY+shipEngineParticleYOffset;
                    g.setClip(engineParticleX, engineParticleY, shipEngineParticleXSize, Math.min(shipEngineParticleYSize, pbottom-engineParticleY));
                    if ((accFrameTime & 16) > 0) engineParticleX -= shipEngineParticleXSize;
                    g.drawImage(shipEngineParticleImage, engineParticleX, engineParticleY, Graphics.TOP|Graphics.LEFT);

                    // ship engine particle
                    shipEngineParticleSpawnTime += lastFrameTime;
                    if (shipEngineParticleSpawnTime > 80) {
                        // create particle
                    }
// HI-RES END
                }
                if (shipBlinkingTime >= 0) shipBlinkingTime -= lastFrameTime;
            }
        }

        // particle-kirakas
// LOW-RES BEGIN
//		g.setClip(0, ptop, scrX, pheight);
//		for (int i = 0; i < particlefTime.length; i++) {
//            int t = particlefTime[i];
//			if (t > 0) {
//                if (t >= 64) {
//                    g.setColor(255, 128+((t-64)<<1), 0);
//                } else {
//                    g.setColor(t<<2, t<<1, 0);
//                }
//                int px = (particlefXPos[i]-fenshift)>>8;
//                int py = particlefYPos[i]>>8;
//                if (t & 1) {
//    				g.fillRect(px, py, 1, 2);
//                } else {
//    				g.fillRect(px, py, 2, 1);
//                }
// LOW-RES END

// HI-RES BEGIN
		for (int i = 0; i < particlefTime.length; i++) {
            int t = particlefTime[i];
			if (t > 0) {
                int px = (particlefXPos[i]-fenshift)>>8;
                int py = (particlefYPos[i]>>8)+ptop;
                g.setClip(px, py, 8, 8);
                g.clipRect(0, ptop, scrX, pheight);
                g.drawImage(particleImages[i%5], px-(particleAnimPos[i]&0xfff8), py, Graphics.TOP|Graphics.LEFT);
                particleAnimPos[i] += lastFrameTime>>1;
                while (particleAnimPos[i] >= 40) particleAnimPos[i] -= 16;
// HI-RES END
			}
		}

        if (shipFlashTime > 0) {
            g.setClip(0, 0, scrX, scrY);

            for (int i = 0; i < shipFlashWidth; i++) {
                int actVal = (shipFlashTime<<1) - (16*i);
                if (actVal < 0) break;

                switch (shipFlashType) {
                    case FLASH_RED:
                        g.setColor(actVal, 0, 0);
                        break;

                    case FLASH_BLUE:
                         g.setColor(0, actVal, actVal);
                        break;

                    case FLASH_WHITE:
                        g.setColor(actVal, actVal, actVal);
                        break;
                }
                g.fillRect(i, ptop2, 4, shipFlashHeight);
                g.fillRect(scrX-i-4, ptop2, 4, shipFlashHeight);
              //  myDisplay.vibrate(10);
            }

            shipFlashTime -= lastFrameTime;
        }

		if (messageTime > 0) {
			messageTime -= lastFrameTime;
			if ((messageTime&64) == 0) writeStringCentered(g, scrX>>1, ptop+(pheight>>1)-fontHeight, messages[messageID]);
		}
	}

	public static void writeNumRight(Graphics g, int x, int y, int num) {
        if (num < 0) return;
		do {
			int digit = num%10;
			num = num/10;

			x -= numberWidth[digit];
			g.setClip(x, y, numberWidth[digit], numberHeight);
			g.drawImage(numberImage, x-numberStart[digit], y, Graphics.TOP|Graphics.LEFT);
		} while (num > 0);
	}

	public static void writeShop(Graphics g, int x, int y, int num) {
        if (num < 0) return;
		do {
			int digit = num%10;
			num = num/10;

			x -= shopNumberWidth[digit];
			g.setClip(x, y, shopNumberWidth[digit], shopNumberHeight);
			g.drawImage(shopNumberImage, x-shopNumberStart[digit], y, Graphics.TOP|Graphics.LEFT);
		} while (num > 0);
	}

	public static void writeNumLeft(Graphics g, int x, int y, int num) {
        if (num < 0) return;
		byte[] digits = new byte[10];
		int p = -1;
		do {
			digits[++p] = (byte)(num%10);
			num = num/10;
		} while (num > 0);

		for (; p >= 0; p--) {
			int digit = digits[p];
			g.setClip(x, y, numberWidth[digit], numberHeight);
			g.drawImage(numberImage, x-numberStart[digit], y, Graphics.TOP|Graphics.LEFT);
			x += numberWidth[digit];
		}
	}

    public static int getCharIndex(char c) {
        for (int i = 0; i < fontChar.length; i++) {
            if (fontChar[i] == c) return i;
        }
        System.out.println("UNKNOWN CHAR AT GETCHARINDEX: "+c+"("+(byte)c+")");     // OPTME: ezt kiszedni
        return 0;   // ignore silently
    }

    public static int getStringWidth(String s) {
		int len = s.length();
		int width = 0;
		for (int i = 0; i < len; i++) {
			int ind = getCharIndex(s.charAt(i));
			width += fontWidth[ind]-1;
		}
        return width;
    }

    public static int getNextLine(String s, int index, int swidth) {
		int len = s.length();
        int width = 0;
        int lastWordEnd = index;
        int ind = 0;
        char ch = 0;

		for (int i = index; i < len; i++) {
            ch = s.charAt(i);
            if (ch == '\\') return i+1;
            if (ch == ' ') lastWordEnd = i;

			ind = getCharIndex(ch);
			width += fontWidth[ind]-1;
            if (width > swidth) return lastWordEnd;
		}
        // az egesz belefer egy sorba
        return len;
    }

	public static void writeStringLeft(Graphics g, int x, int y, String s) {
        g.setClip(0, 0, scrX, scrY);
		int len = s.length();
		for (int i = 0; i < len; i++) {
			int ind = getCharIndex(s.charAt(i));
            int fwidth = fontWidth[ind];
            if (fwidth+x >= 0 && x < scrX) {
                g.setClip(x, y, fwidth, fontHeight);
                g.drawImage(fontImage, x-fontStart[ind], y, Graphics.TOP|Graphics.LEFT);
                //g.drawImage(fontCharImage[ind], x, y, Graphics.TOP|Graphics.LEFT);
            }
            x += fwidth-1;
		}
	}

	public static void writeStringCentered(Graphics g, int x, int y, String s) {
		writeStringLeft(g, x-(getStringWidth(s)>>1), y, s);
	}

	public static void writeStringRight(Graphics g, int x, int y, String s) {
		writeStringLeft(g, x-getStringWidth(s), y, s);
	}

/**********************************************************************************
/* EVENT
/**********************************************************************************/
	final class InputEvent {
		public int start;
		public int length = -1;
		public int key;
		public boolean repeated = false;
	}

	static InputEvent[] ehie = new InputEvent[16];  // ennyi ugyse lesz
	public static int ehFirst = 0, ehLast = 0;

	static public int ehSize() {
		return ehLast-ehFirst;
	}

	static public InputEvent ehNext() {
		if (ehFirst == ehLast) return null;
		InputEvent ret = ehie[ehFirst&0xf];
		ehie[ehFirst&0xf] = null;	// DEBUG
		ehFirst++;
		return ret;
	}

	static public void ehAdd(InputEvent e) {
		if (ehLast >= 16 && ehFirst >= 16) {
			ehLast &= 15; ehFirst &= 15;
		}
		ehie[ehLast&0xf] = e;
		ehLast++;
	}

	static public InputEvent ehSearch(int key) {
		for (int i = ehFirst; i < ehLast; i++) {
			int j = i & 0xf;
			if (ehie[j].key == key && ehie[j].length < 0) return ehie[j];
		}
		return null;
	}

	public void keyPressed(int i) {
		int time = (int)(System.currentTimeMillis()-levelStartTime);
        int key = i;
        if (i == -6 || i == -11) key = Canvas.KEY_STAR;
        else if (i == -7) key = Canvas.KEY_POUND;
        else key = getGameAction(i);
        // softkey - nonstandard!
		switch (state) {
			case STATE_MENU_TO_GAME:
			case STATE_GAME_TOP_IN:
			case STATE_GAME:
				if (key==Canvas.KEY_STAR&&state==STATE_GAME) {
					state = STATE_GAME_QUITQUESTION;
				} else {
					//synchronized(ehie) {	// todo: nem kell synch, ha callserially() van
						InputEvent nie = new InputEvent();
						nie.start = time;
						nie.key = key;
						ehAdd(nie);
					//}
				}
				break;
			case STATE_GAME_QUITQUESTION:
				if (key==Canvas.KEY_STAR) {
					state = STATE_GAME_TOP_OUT;
					nextState = STATE_MENU_DOOR_OUT;
				}
				if (key==Canvas.KEY_POUND) {
					state = STATE_GAME;
				}
				break;
                
            case STATE_MENU_NEWQUESTION:
                if (key == Canvas.KEY_STAR) {
                    resetGame();
                    prevState = state;
                    state = STATE_MENU;
                }
                if (key == Canvas.KEY_POUND) {
                    // jump back to 'GAME'
                    menuParent = 0;
                    menuSelect = 0;
                    menuOffset = 5;
                    prevState = state;
                    state = STATE_MENU;
                }
                break;
                
			case STATE_MENU_UPGRADE:
			case STATE_MENU:
				if (key != 0) moveMenu(key);
                else moveMenu(i);
				break;
                
			case STATE_MENU_CHAPTER:
				if ((key == Canvas.LEFT || key == Canvas.KEY_STAR) && (selectedChapter>0||chapterBigImageX<0)) {
					chapterScrollDelta=1;
				}
				if ((key == Canvas.RIGHT || key == Canvas.KEY_POUND) && (selectedChapter<9||chapterBigImageX>0)) {
					chapterScrollDelta=-1;
				}

				if (key==Canvas.FIRE&&chapterScrollDelta==0) {
					if (selectedChapter <= maxLevelNum) {
                        levelNum = selectedChapter;
                        state = STATE_MENU_CHAPTER_CLOSE;
                    }
				}
				break;

            case STATE_SCORES:
                switch (key) {
                    case Canvas.KEY_POUND:
                    case Canvas.KEY_STAR:
                        if (editingHighScore >= 0) {
                            highScoreNames[editingHighScore] = new String(editingName);
                            saveHighScore();
                        }
                        state = STATE_SCORES_TO_MENU;
                        accMenuTime = 0;
                        break;

                    default:
                        if (editingHighScore >= 0) {
                            int actChar;
                            switch (key) {
                                case Canvas.UP:
                                    actChar = getCharIndex(editingName[editingHighScorePos]);
                                    if (actChar == 0) actChar = fontChar.length-1;
                                    else actChar--;
                                    editingName[editingHighScorePos] = fontChar[actChar];
                                    break;

                                case Canvas.DOWN:
                                    actChar = getCharIndex(editingName[editingHighScorePos]);
                                    actChar++;
                                    if (actChar >= fontChar.length) actChar -= fontChar.length;
                                    editingName[editingHighScorePos] = fontChar[actChar];
                                    break;

                                case Canvas.LEFT:
                                    if (editingHighScorePos > 0) editingHighScorePos--;
                                    break;

                                case Canvas.RIGHT:
                                    if (editingHighScorePos < 8) editingHighScorePos++;
                                    break;
                            }
                        }

                }
                break;

            case STATE_STORY:
                if (key == Canvas.KEY_STAR && actStoryPage > 0) {
                    prevStoryPage = actStoryPage;
                    actStoryPage--;
                    accMenuTime = 0;
                }
                if (key == Canvas.KEY_POUND) {
                    if (actStoryPage < lastStoryPage) {
                        prevStoryPage = actStoryPage;
                        actStoryPage++;
                    } else {
                        leaveStory();
                    }
                    accMenuTime = 0;
                }
                if (key == Canvas.FIRE) {
                    leaveStory();
                    accMenuTime = 0;
                }
                break;
		}
	}

    public static void leaveStory() {
        if (levelNum == 15) {
            // EVENT: endsequence
            state = STATE_STORY_TO_SCORES;
            highScorePossible = true;
        } else if (actStory == storyBeforeLevel[levelNum]) {
            // EVENT: STORY->GAME, storyBeforeLevel
            state = STATE_STORY_TO_GAME;
        } else {
            // EVENT: STORY->MENU, storyAtferLevel
            state = STATE_STORY_TO_MENU;
        }
    }

	public void keyRepeated(int i) {
		if (state == STATE_MENU || state == STATE_MENU_UPGRADE) {
			moveMenu(getGameAction(i));
		}
	}

	public void keyReleased(int i) {
		if (state == STATE_GAME || state == STATE_GAME_TOP_IN || state == STATE_MENU_TO_GAME) {
			int time = (int)(System.currentTimeMillis()-levelStartTime);
			int key = getGameAction(i);
			//synchronized(ehie) {
				InputEvent eie = ehSearch(key);
				if (eie != null) eie.length = (int)(time-eie.start);
			//}
		}
	}


/**********************************************************************************
/* ACTORS
/**********************************************************************************/
final static class Enemy {
	public int bgPos, hp, type, dx, dy;
    public int lastShot, bulletFreq;
	final EnemyType entype;
	final Path p;
	public int pathPoint = -1;	// todo: def. erteke 0 legyen, a konstruktort ennek megfeleloen javitani
	int fxPoint, fyPoint, fdAct, fwait;
	public int fxAct, fyAct;
	public int explosionTime = -1;
	public int flashTime = 0;
	public Enemy[] shadows;
	public int fxGhost, fyGhost, ghostTime = -1;
	public byte[] actorParams;

	public Enemy(int _type, int _bgPos, int _path, int _dx, int _dy, EnemyType _entype) {
		type = _type;
		entype = _entype;
		bgPos = _bgPos;
		dx = _dx;
		dy = _dy;
		hp = entype.hp;
        if (entype.bulletType >= 16) {      // tud-e loni az ellen vagy sem
            bulletFreq = entype.bulletFreq;
        } else {
            lastShot = -1;      // mert ha nem,
        }

		if (_path < pathes.length) p = pathes[_path];
		else p = null;

		if (type == 9) {
			shadows = new Enemy[3];
			shadows[0] = new Enemy(59, bgPos, _path, dx, dy, entype);
			shadows[1] = new Enemy(59, bgPos, _path, dx, dy, entype);
			shadows[2] = new Enemy(59, bgPos, _path, dx, dy, entype);
		}
        if (type == 19) {
            actorParams = new byte[2];
            dx = 0;
        }
	}

	public Enemy(int _type, int _bgPos, byte[] _actorParams) {
		type = _type;
		entype = null;
		p = null;
		bgPos = _bgPos;
		actorParams = _actorParams;
	}

	void activate() {
		if (p == null) {					// stationary
			if (type==17) {
				fxAct = (bgLayers[bgLayers.length-1].getXPosFor(dx))<<8;
				fyAct = (enbgpos - bgPos)<<8;
			} else {
				fxAct = (bgLayers[bgLayers.length-2].getXPosFor(dx))<<8;
				fyAct = (enbgpos - bgPos)<<8;
			}
			pathPoint = 0;
			return;

		} else if (type == 10) {			// ghost - az 1. pontra ugrik be
			fxAct = fyAct = fxPoint = fyPoint = -128*256;
			ghostTime = 0;
			fxGhost = (dx<<8)/192;
			fyGhost = dy + p.y[0];
			
			System.out.println("Activating; fxGhost: "+fxGhost+", fyGhost: "+fyGhost+", dx: "+dx+", dy: "+dy+" bgPos: "+bgPos+", p.x[0]: "+p.x[0]+", p.y[0]:"+p.y[0]+", p.dx[0]: "+p.dx[0]+", p.dy[0]: "+p.dy[0]);

		} else {

			fxAct = fxPoint = (dx<<8)/192;
			fyAct = fyPoint = (((dy-enHeight[type])<<16)/pheight)>>8;	// hogy ne ugorjon be a kepbe semmilyen felbontas mellett sem
		}

		pathPoint = 0;
		fwait = p.wait[0];

		if (type == 9) {
			//System.out.println("act");
			for (int i = 0; i < 3; i++) {
				shadows[i].fxPoint = fxPoint;
				shadows[i].fyPoint = fyPoint;
				shadows[i].fxAct = fxAct;
				shadows[i].fyAct = fyAct;
				shadows[i].pathPoint = pathPoint;
				shadows[i].fwait = 32*(i+1);
			}
		}

		int temp1 = bgPos - enHeight[type] - pheight - ((fAccBGTime*fEnemyBGSpeed)>>16);
		//System.out.println("temp1: "+temp1+", enbgpos: "+enbgpos+", accfrtime: "+accFrameTime);

		int temp2 = (int)(((long)temp1<<16)/fEnemyBGSpeed);
		//System.out.println("temp2: "+temp2+", fEnemyBGSpeed: "+fEnemyBGSpeed);

		int time = accFrameTime - temp2;
		//System.out.println("final time: "+time);
		move(time);
	}

	void move(int ft) {
		if (p == null) {				// stationary
			fyAct = (enbgpos - bgPos)<<8;
			return;

		} else if (shadows != null) {	// fast enemy
			shadows[0].move(ft);
			shadows[1].move(ft);
			shadows[2].move(ft);
		} else if (ghostTime >= 0) {	// ghost-fade van
			ghostTime += ft;
			if (ghostTime >= 96) {		// fade vege
				ft -= (ghostTime-96);
				ghostTime = -1;
				fdAct = 0;
				fxPoint = fxGhost;
				fyPoint = fyGhost;
				fxAct = fxPoint*penwidth;
				fyAct = fyPoint*pheight;

				if (pathPoint < p.length) {
					pathPoint++;
					fwait = p.wait[pathPoint];
				} else {
					return;		// and die...
				}
			}
		}

		if (fwait > 0) {
			if (fwait > ft) {
				fwait -= ft;
				//return;
				ft = 0;
			} else {
				ft -= fwait;
				fwait = 0;
			}
		}

		int fdAdv = (ft*entype.speed*p.speed[pathPoint])>>8;
		int fdRem = p.fDist[pathPoint]-fdAct;

		//System.out.println("fdAdv: "+fdAdv+", fdRem: "+fdRem+", speed: "+entype.speed+", p.speed:"+p.speed[pathPoint]);
		while (fdAdv > fdRem) {
			fdAdv -= fdRem;

            //System.out.println(""+fdAdv+", "+fdRem+", "+p.fDist[pathPoint]+", "+fdAct+", "+pathPoint);

			fxPoint += p.dx[pathPoint];
			fyPoint += p.dy[pathPoint];

			if (pathPoint < p.length || type >= 20) {
				pathPoint++;
                if (type >= 20 && pathPoint >= p.length) pathPoint -= p.length;
				fwait = p.wait[pathPoint];
				if (fwait > 0) {
					int fremft = (fdAdv<<8)/(entype.speed*p.speed[pathPoint]);
					if (fremft > fwait) {
						fremft -= fwait;
						fwait = 0;
						fdAdv = (fremft*entype.speed*p.speed[pathPoint])>>8;
					} else {
						fwait -= fremft;
						fdAdv = 0;
					}
				}
			}

			fdAct = 0;
			fdRem = p.fDist[pathPoint];
		}

		// itt mar vagy elert a path vegere, vagy elfogyott az advance
		fdAct += fdAdv;
		if (type == 10 && ghostTime < 0 && (fdRem>>2 < fdAct)) {		// ugras
			//System.out.println("Dist done, ghosting");
			ghostTime = 0;
			if (pathPoint < p.length) {
				fxGhost += p.dx[pathPoint];
				fyGhost += p.dy[pathPoint];
			} else {
				fxGhost = fyGhost = -128*256;
			}
		}

		int fdRatio = (fdAct<<8)/p.fDist[pathPoint];
        //System.out.println("fdRatio: "+fdRatio+", fdAct: "+fdAct+", fDist: "+p.fDist[pathPoint]+", dx: "+p.dx[pathPoint]+", dy: "+p.dy[pathPoint]);
		//System.out.println(" >>> "+(fxPoint>>8));
		fxAct = (((fxPoint<<8) + (fdRatio*p.dx[pathPoint]))*penwidth)>>8;
		fyAct = (((fyPoint<<8) + (fdRatio*p.dy[pathPoint]))*pheight)>>8;
		//System.out.println(" >>> "+(fxAct>>8)+", "+fyAct+", time: "+ft);
	}
}

/**********************************************************************************
/* LAYERS
/**********************************************************************************/
// TODO: bg hossz check (ha elerte a palya veget, ne dobja el magat!)
interface Drawable {
	void draw(Graphics g);
	int getXPosFor(int xpos);
	//void setSpeed(int newSpeed);
    void setTopLayer();
}

final class CenteredLevelLayer implements Drawable {
	final byte[] bg;
	public int speed;
	//int accBGPos = 0;
    boolean topLayer = false;

	public CenteredLevelLayer(byte[] _bg, int _speed) {
		speed = _speed;
		bg = _bg;
		if (speed==4608) topLayer=true; // hakkolas
	}

    public void setTopLayer() {
        topLayer = true;
    }

	public int getXPosFor(int xpos) {
		return penmiddle-(96-xpos);
	}

/*	public void setSpeed(int newSpeed) {
		accBGPos += accFrameTime*speed;
		speed = newSpeed;
	}*/

	public void draw(Graphics g) {
		// clip
		g.setClip(pleft, ptop, pwidth, pheight);

		// bgpos ujrakalkulalasa
		int bgPos = (int)(((accFrameTime+fAccBGTime)*speed)>>16);

		int bgPosMod = bgPos&0x1f;
		int bgPosDiv = bgPos>>5;
		int vscrleft = vscrl + (topLayer ? enshift-bgshift : (((vscrdelta*speed)+32768)>>16));

		// kirajzolas
		int bgIndex = bgPosDiv*12;
		for (int i = 1; i <= tileHeightToDraw; i++) {
			int topPos = i<<5;

            if (bgIndex >= bg.length) {     // palya vegtelenites
                bgIndex %= bg.length;
            }

			for (int j = 0; j < 12; j++) {
				int actImIndex = bg[bgIndex++];
				if (actImIndex < 0 || actImIndex >= bgImages.length) continue;

				// Y irany check
				if (topPos-bgHeight[actImIndex] > pheight+bgPosMod) continue;

				// X irany check
				int leftPos = (j << 4)-vscrleft;
				if (leftPos >= pright || bgWidth[actImIndex]+leftPos < pleft) continue;

				g.drawImage(bgImages[actImIndex], pleft+leftPos, pbottom + bgPosMod - topPos, Graphics.TOP|Graphics.LEFT);
			}
		}
	}
}

final class RepeatedLevelLayer implements Drawable {
	final byte[] bg;
	public int speed, tileWidthToDraw, startTile, endTile;
	//int accBGPos = 0;
    boolean topLayer = false;

	public RepeatedLevelLayer(byte[] _bg, int _speed) {
		speed = _speed;
		bg = _bg;

		int actwidth = pwidth+((maxBGShift*((speed<<8)/fMaxBGSpeed))>>7);
		tileWidthToDraw = actwidth>>4;
		if ((pwidth&0xf) != 0) tileWidthToDraw++;

		startTile = 6-(tileWidthToDraw>>1);
		endTile = 6+(tileWidthToDraw>>1);
		if (speed==4608) topLayer=true; // hakkolas
	}

    public void setTopLayer() {
        topLayer = true;
    }

    public int getXPosFor(int xpos) {
		return penmiddle-(96-xpos);
	}

/*	public void setSpeed(int newSpeed) {
		accBGPos += accFrameTime*speed;
		speed = newSpeed;
	}*/

	public void draw(Graphics g) {
		// clip
		g.setClip(pleft, ptop, pwidth, pheight);

		// bgpos ujrakalkulalasa
		int bgPos = (int)(((accFrameTime+fAccBGTime)*speed)>>16);

		int bgPosMod = bgPos&0x1f;
		int bgPosDiv = bgPos>>5;
		int vscrleft = vscrl + (topLayer ? enshift-bgshift : (((vscrdelta*speed)+32768)>>16));

        // kirajzolas
		for (int i = 1; i <= tileHeightToDraw; i++) {
			int bgIndex = (bgPosDiv+i-1)*12;

            if (bgIndex >= bg.length) {     // palya vegtelenites
                bgIndex %= bg.length;
            }
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
				int leftPos = (j << 4)-vscrleft;
				if (leftPos >= pright || bgWidth[actImIndex]+leftPos < pleft) continue;

				g.drawImage(bgImages[actImIndex], pleft+leftPos, pbottom + bgPosMod - topPos, Graphics.TOP|Graphics.LEFT);
			}
		}
	}
}

final class JustifiedLevelLayer implements Drawable {
	final byte[] bg;
	public int speed;
	int maxShift, justCenterTile;
	//int accBGPos = 0;
    boolean topLayer = false;

	public JustifiedLevelLayer(byte[] _bg, int _speed) {
		speed = _speed;
		bg = _bg;
		if (speed==4608) topLayer=true; // hakkolas
	}

    public void setTopLayer() {
        topLayer = true;
    }

    public void postInit() {
		maxShift = ((maxBGShift*((speed<<8)/fMaxBGSpeed))+128)>>8;

		justCenterTile = (pwidth+maxShift)>>5;
		if (((pwidth+maxShift)&0x1f) != 0) justCenterTile++;
		if (justCenterTile > 6) justCenterTile = 6;
	}

	public int getXPosFor(int xpos) {
		if (xpos < penmiddle) {
			return xpos;
		} else if (xpos >= (192-penmiddle)) {
			return penwidth-(192-xpos);
		} else {
			return -1;
		}
	}

/*	public void setSpeed(int newSpeed) {
		accBGPos += accFrameTime*speed;
		speed = newSpeed;
	}*/

	public void draw(Graphics g) {
		// bgpos ujrakalkulalasa
		int bgPos = (int)(((accFrameTime+fAccBGTime)*speed)>>16);

		int bgPosMod = bgPos&0x1f;
		int bgPosDiv = bgPos>>5;
		int vscrleft = topLayer ? enshift - bgshift : (((vscrdelta*speed)+32768)>>16);
        //System.out.println("vscrleft: "+vscrleft+" ("+enshift+" - "+bgshift+")");

        // kirajzolas
		// bal oldal
		g.setClip(pleft, ptop, pwidth>>1, pheight);
		for (int i = 1; i <= tileHeightToDraw; i++) {
			int bgIndex = (bgPosDiv+i-1)*12;
            if (bgIndex >= bg.length) {     // palya vegtelenites
                bgIndex %= bg.length;
            }
			int topPos = i<<5;

			for (int j = 0; j < justCenterTile; j++) {
				int actImIndex = bg[bgIndex++];
				if (actImIndex < 0 || actImIndex >= bgImages.length) continue;

				// Y irany check
				if (topPos-bgHeight[actImIndex] > pheight+bgPosMod) continue;

				int leftPos = (j<<4)-maxShift-vscrleft;

				g.drawImage(bgImages[actImIndex], pleft+leftPos, pbottom + bgPosMod - topPos, Graphics.TOP|Graphics.LEFT);
			}
		}

		//jobb oldal
		g.setClip(pwidth>>1, ptop, pwidth>>1, pheight);
		for (int i = 1; i <= tileHeightToDraw; i++) {
			int bgIndex = (bgPosDiv+i-1)*12+6;
            if (bgIndex >= bg.length) {     // palya vegtelenites
                bgIndex %= bg.length;
            }
			int topPos = i<<5;

			for (int j = 6; j < 12; j++) {
				int actImIndex = bg[bgIndex++];
				if (actImIndex < 0 || actImIndex >= bgImages.length) continue;

				// X irany check
				int leftPos = pwidth+maxShift-vscrleft - ((12-j)<<4);
				if (leftPos >= pright || bgWidth[actImIndex]+leftPos < pleft+(pwidth>>2)) continue;

				// Y irany check
				if (topPos-bgHeight[actImIndex] > pheight+bgPosMod) continue;

				g.drawImage(bgImages[actImIndex], pleft+leftPos, pbottom + bgPosMod - topPos, Graphics.TOP|Graphics.LEFT);
			}
		}
	}
}
}

final class Bullet {
	public boolean enemy;
	public int fxAct, fyAct, angle, speed, type, actFrame, accFrameTime, fdx, fdy, fd, damage;
    public int fPhase = -1;
	final int actblfrnum;
    public int spawnParticle = 0;

	public Bullet(boolean _enemy, int _fxAct, int _fyAct, int _angle, int _speed, int _type, int _damage) {
		enemy = _enemy;
		fxAct = _fxAct;
		fyAct = _fyAct;
		speed = (_speed * MyCanvas.bulletSpeed)>>8;
		angle = _angle;
		type = _type;
		damage = _damage;

		actblfrnum = MyCanvas.bulletFrameNum[type];
		fxAct -= (MyCanvas.fBulletWidth[type]>>1);
		//fyAct -= (MyCanvas.fbulletHeight[type]>>1);	// nem kell, mert alapbol lejjebbrol kell inditani a lovedekeket - kb. pont ennyivel
	}

	void move(int ft) {
		// lovedek frame
		if (actblfrnum > 1) {
			accFrameTime += ft;
            spawnParticle += ft;
			actFrame += accFrameTime>>5;
			accFrameTime &= 0x1f;
			while (actFrame >= actblfrnum) {
                actFrame -= actblfrnum;
            }
		}

        // raketa
        if (fPhase >= 0) {
            int oldph = Stuff.sin(fPhase>>8);
            fPhase += ft*90;   // hullamhossz
            while (fPhase >= (360<<8)) fPhase -= 360<<8;
            int actph = Stuff.sin(fPhase>>8);
            fxAct += ((actph-oldph)*8)>>8;
            fyAct += ft*speed;
            return;
        }

		// mozgas
		int d = ft*speed;
		if (d == 0) return;

		if (angle >= 0) {
			fxAct += (Stuff.cos(angle)*d)>>16;
			fyAct -= (Stuff.sin(angle)*d)>>16;
		} else {
			while (d > fd) {
				fxAct += fdx;
				fyAct += fdy;
				d -= fd;
			}

			int ratio = (d<<8)/fd;
			fxAct += (ratio*fdx)>>8;
			fyAct += (ratio*fdy)>>8;
		}

//        System.out.println("Bullet X: "+(fxAct>>8)+", Y: "+(fyAct>>8));
	}

	void seek(int mindx, int mindy, int mind, int ft) {
		angle = -1;
		fdx = mindx;
		fdy = mindy;
		fd = mind;

		int actd = ft*speed;
		if (actd >= mind) {
			fxAct += mindx;
			fyAct += mindy;
		} else {
			int ratio = (actd<<8)/mind;
			fxAct += (ratio*mindx)>>8;
			fyAct += (ratio*mindy)>>8;
		}
	}
}

/**********************************************************************************
/* PATH
/**********************************************************************************/
final class Path {
	public final int[] x, y, speed, wait, fDist, dx, dy;
	public final int length;

	public Path(int numPoints) {
		x = new int[numPoints];
		y = new int[numPoints];
		dx = new int[numPoints];
		dy = new int[numPoints];
		speed = new int[numPoints];
		wait = new int[numPoints];
		fDist = new int[numPoints];
		length = numPoints-1;
	}

	public void init() {
		for (int i = 0; i < length; i++) {
			dx[i] = x[i+1]-x[i];
			dy[i] = y[i+1]-y[i];

			fDist[i] = Stuff.sqrt(dx[i]*dx[i] + dy[i]*dy[i])<<8;	// tavolsag, 8bites fixpontban
            //System.out.println("i: "+i+", dx: "+dx[i]+", dy: "+dy[i]+", fDist: "+fDist[i]);
		}

		// utvonal vegeztevel palyan marad
		dx[length] = dx[length-1];
		dy[length] = dy[length-1];
		fDist[length] = fDist[length-1];
	}
}

final class EnemyType {
	public int imageNum, speed, hp, point, bulletType, bulletFreq;

	public EnemyType(int _imageNum, int _speed, int _hp, int _point, int _bulletType, int _bulletFreq) {
		imageNum = _imageNum;
		speed = _speed;
		hp = _hp;
		point = _point;
		bulletType = _bulletType;
		bulletFreq = _bulletFreq;
	}
}


// TODO: pause & stop-ot implementalni
public class MyMIDlet extends MIDlet {
	MyCanvas canvas;

	public MyMIDlet() {
	}

	public void startApp() {
		if (canvas == null) {
            try {
    			canvas = new MyCanvas();
                // jit compile - FAILED
//                for (int i = 0; i < 500; i++) {
//                    for (int j = 0; j <= 28; j++) {
//                        canvas.fireWeapon((i&1)==1, 40<<8, 40<<8, j);
//                        for (int k = 0; k < canvas.bulletLast; k++)
//                            canvas.bullets[k] = null;
//                    }
//                }
    		} catch (Exception e) {
    			e.printStackTrace();
    		}

            canvas.myDisplay = Display.getDisplay(this);
            canvas.myMIDlet = this;
            canvas.myDisplay.setCurrent(canvas);
    		canvas.init();
        } else {
            canvas.myDisplay = Display.getDisplay(this);
            canvas.myDisplay.setCurrent(canvas);
            canvas.ehFirst = canvas.ehLast;	// reset keys
            if (canvas.state == MyCanvas.STATE_GAME) {
                canvas.state = MyCanvas.STATE_GAME_QUITQUESTION;
            }
            try {
                canvas.player.start();
            } catch (Exception e) {}
        }
	}

	public void pauseApp() {
        try {
            canvas.player.stop();
        } catch (Exception e) {}
	}

	public void destroyApp(boolean unconditional) {
	}
}
