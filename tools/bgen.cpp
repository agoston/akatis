#include <stdio.h>
#include <string.h>

static const int width = 10;
static const int height = 10000;
char field[height][width];

int main(int argc, char *argv[]) {
	if (argc<3) return 1;

	FILE *f = fopen(argv[1], "rt");

	memset(field, ' ', sizeof(field));

	int x=0, y=0;
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
		x++;
	}
	fclose(f);

	int len = width*y;
	unsigned char c0 = len&0xff;
	unsigned char c1 = len>>8;
	f = fopen(argv[2], "wb");
	fwrite(&c0, 1, 1, f);
	fwrite(&c1, 1, 1, f);
	fwrite(field, 1, width*y, f);
	fclose(f);
}
