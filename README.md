android-navigation-library
==========================

Android Navigation Library - simple navigation from current location to a destination

Example:
Intent intent = Intent(this,NavigationActivity.class);
intent.putExtra(NavigationActivity.EXTRA_DESTINATION_LATITUDE, (double) 8.49975);
intent.putExtra(NavigationActivity.EXTRA_DESTINATION_LONGITUDE, (double) 124.625985);
startActivity(intent);