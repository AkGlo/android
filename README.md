## About the Librelio Magazine Solution
The Librelio Magazine Solution is intended mainly for magazine publishers. It consists in:
- customizable mobile apps: a complete version has been developed on [iOS](https://github.com/libreliodev/iOS), Android (here) and  on  [Windows 8](https://github.com/libreliodev/windows8).
- a cloud based server

Examples of apps developed using this solution are listed on www.librelio.com . A good example of iOS app is the [Wind magazine app](https://itunes.apple.com/fr/app/wind-magazine/id433594605?mt=8). 


## License
This project is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This project is Copyright 20012-2013 WidgetAvenue - Librelio, except included libraries:
- Copyright 2006-2012 Artifex Software, Inc for the MuPDF library
- Copyright Free Beachler (http://www.freebeachler.com) for the Android P-List Parser library

The use of the Google Analytics library is governed by the following terms: http://www.google.com/analytics/tos.html



## Source code of libraries used
- MuPDF: https://github.com/libreliodev/mupdf.git
- Android P-List Parser: https://github.com/libreliodev/android-plist-parser

## Customization of the app
#### 1- Replace the following elements with your custom elements:
*  assets directory
*  in res directory, all subdirectories ending with -port or -land
*  in res/values directory, application_.xml

#### 2- Replace ic_launcher.png with your own icon, in different sizes:
* drawable-mdpi - 48 x 48px
* drawable-hdpi - 72 x 72px
* drawable-xhdpi - 96 x 96px
* drawable-xxhdpi - 144 x 144px


#### 3- Rename the package in Eclipse:
As explained on [Stack Overflow](http://stackoverflow.com/questions/3697899/package-renaming-in-eclipse-android-project), press F2 on package name. 


#### 4- Update AndroidManifest.xml:
Enter the new package name.
