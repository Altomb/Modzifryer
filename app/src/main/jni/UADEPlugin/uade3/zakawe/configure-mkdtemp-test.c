#include <stdlib.h>
#include <unistd.h>

int main(void)
{
	mkdtemp("/tmp/tmp.XXXXXX");
	return 0;
}
