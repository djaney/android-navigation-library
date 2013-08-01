android-navigation-library
==========================

Android Navigation Library - use by extending NavigationActivity and supplying it's intent with double extras (NavigationActivity.EXTRA_DESTINATION_LATITUDE,NavigationActivity.EXTRA_DESTINATION_LONGITUDE)

Example:
Intent intent = Intent(this,NavigationActivity.class);
intent.putExtra(NavigationActivity.EXTRA_DESTINATION_LATITUDE, (double) 8.49975);
intent.putExtra(NavigationActivity.EXTRA_DESTINATION_LONGITUDE, (double) 124.625985);
startActivity(intent);