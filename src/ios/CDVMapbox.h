#import <Cordova/CDVPlugin.h>
#import "Mapbox.h"
#import "SVGgh.h"

@interface CDVMapbox : CDVPlugin<MGLMapViewDelegate>

@property (retain) MGLMapView *mapView;
@property (retain) NSString *markerCallbackId;
@property (retain) MGLPointAnnotation *selectedAnnotation;
@property (retain) NSString *regionWillChangeAnimatedCallbackId;
@property (retain) NSString *regionIsChangingCallbackId;
@property (retain) NSString *regionDidChangeAnimatedCallbackId;
@property (retain) NSString *onSetMyLocationCallbackId;



- (void) show:(CDVInvokedUrlCommand*)command;
- (void) hide:(CDVInvokedUrlCommand*)command;

- (void) addMarkers:(CDVInvokedUrlCommand*)command;
- (void) showMarkerAnnotation:(CDVInvokedUrlCommand*)command;


- (void) removeAllMarkers:(CDVInvokedUrlCommand*)command;
- (void) addMarkerCallback:(CDVInvokedUrlCommand*)command;

- (void) animateCamera:(CDVInvokedUrlCommand*)command;

- (void) addPolygon:(CDVInvokedUrlCommand*)command;

- (void) addGeoJSON:(CDVInvokedUrlCommand*)command;

- (void) getCenter:(CDVInvokedUrlCommand*)command;
- (void) setCenter:(CDVInvokedUrlCommand*)command;

- (void) getZoomLevel:(CDVInvokedUrlCommand*)command;
- (void) setZoomLevel:(CDVInvokedUrlCommand*)command;

- (void) getTilt:(CDVInvokedUrlCommand*)command;
- (void) setTilt:(CDVInvokedUrlCommand*)command;

- (void) convertCoordinate:(CDVInvokedUrlCommand*)command;
- (void) convertPoint:(CDVInvokedUrlCommand*)command;

- (void) onRegionWillChange:(CDVInvokedUrlCommand*)command;
- (void) onRegionIsChanging:(CDVInvokedUrlCommand*)command;
- (void) onRegionDidChange:(CDVInvokedUrlCommand*)command;


- (void) addMarker:(CDVInvokedUrlCommand*)command;

- (void) addToggleButton:(CDVInvokedUrlCommand*)command;

@end

@protocol MapboxAnnotationWithImage <MGLAnnotation>

@required
@property (nonatomic) NSDictionary* imageData;

@end

@interface MapboxPointAnnotationWithImage: MGLPointAnnotation <MapboxAnnotationWithImage>

@end

