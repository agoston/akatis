#include <stdio.h>

char buffer[10000];

int main() {
	FILE *f = fopen("enemy.txt", "rt");
	int pos=0;

	char *b = buffer+2;
	for (;;) {
		int dpos, type, path, dx, dy;
		if (feof(f)) break;

		dx = 0;
		dy = 0;

		if (fscanf(f, "%d %d %d %d %d\n", &dpos, &type, &path, &dx, &dy)==5) {
			pos += dpos;
			*b++ = pos&0xff;
			*b++ = pos>>8;
			*b++ = type;
			*b++ = path;
			*b++ = dx;
			*b++ = dy;
		}
	}

	fclose(f);

	int len = b-buffer-2;
	buffer[0] = len&0xff;
	buffer[1] = len>>8;
	f = fopen("enemy", "wb");
	fwrite(buffer, 1, len+2, f);
	fclose(f);
}
