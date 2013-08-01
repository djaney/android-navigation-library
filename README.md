android-navigation-library
==========================

<<<<<<< HEAD
Android Navigation Library - simple navigation from current location to a destination
=======
Android Navigation Library - use by extending NavigationActivity and supplying it's intent with double extras (NavigationActivity.EXTRA_DESTINATION_LATITUDE,NavigationActivity.EXTRA_DESTINATION_LONGITUDE)
>>>>>>> d20744485a02fc94a8350c635809d39f2bef6af5

Example:
Intent intent = Intent(this,NavigationActivity.class);
intent.putExtra(NavigationActivity.EXTRA_DESTINATION_LATITUDE, (double) 8.49975);
intent.putExtra(NavigationActivity.EXTRA_DESTINATION_LONGITUDE, (double) 124.625985);
startActivity(intent);