#include <stdio.h>
#include <string.h>

static const int width = 28;
static const int height = 28;
char field[height][width];
signed char path[10000];

int abs(int x) {
	if (x<0) return -x;
	return x;
}

int main(int argc, char *argv[]) {
	if (argc<3) return 1;

	FILE *f = fopen(argv[1], "rt");

	memset(field, ' ', sizeof(field));

	int x=0, y=0;
	int bx=-1, by;
	for (;;) {
		int c = fgetc(f);

		if (c==-1) break;

		if (c=='\n') {
			y++;
			x=0;
			continue;
		}
		if (c=='\r') continue;

		if (x>=width) {
			fprintf(stderr, "too long line\n");
			return 1;
		}
		if (y>=height) {
			fprintf(stderr, "too many lines\n");
			return 1;
		}
		field[y][x] = c;
		if (c=='A') {
			if (bx!=-1) {
				fprintf(stderr, "too many A\n");
				return 1;
			}
			bx=x;
			by=y;
		}
		x++;
	}
	fclose(f);

	if (bx==-1) {
		fprintf(stderr, "A not found\n");
		return 1;
	}

	signed char *p = path+2;
	*p++ = bx*8-128;
	*p++ = by*8-128;
	int n = 'B';
	for (;;) {
		field[by][bx] = ' ';

		int v = 1000;
		int nx, ny;
		for (int y=by-1; y<=by+1; y++) {
			if (y<0||y>=height) continue;
			for (int x=bx-1; x<=bx+1; x++) {
				if (x<0||x>=width) continue;

				if (field[y][x]>='a'&&field[y][x]<=v) {
					if (field[y][x]==v) {
						fprintf(stderr, "cannot decide at %d:%d\n", bx, by);
						return 1;
					}
					v = field[y][x];
					nx = x;
					ny = y;
				}
			}
		}

		if (v==1000) {
			nx=-1;
			for (int y=0; y<height; y++) {
				for (int x=0; x<width; x++) {
					if (field[y][x]==n) {
						if (nx!=-1) {
							fprintf(stderr, "multiple %c\n", n);
							return 1;
						}
						nx = x;
						ny = y;
					}
				}
			}
			if (nx==-1) {
				break;
			}
			n++;
		}
		if (abs(nx-bx)<=1&&abs(ny-by)<=1) {
			int X = nx-bx+1;
			int Y = ny-by+1;

			char dir[9] = { 7, 6, 5, 8, -1, 4, 1, 2, 3 };
			char d = dir[Y*3+X];
			if (d==-1) {
				fprintf(stderr, "he?");
				return 1;
			}
			*p++ = d;
		} else {
			*p++ = 0;
			*p++ = nx*8-128;
			*p++ = ny*8-128;
		}
		bx = nx;
		by = ny;
	}
	int len = p-path-2;
	path[0] = len&0xff;
	path[1] = len>>8;

	f = fopen(argv[2], "wb");
	fwrite(path, 1, len+2, f);
	fclose(f);
}
