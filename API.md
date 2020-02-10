# API

The server handles [IIIF Image API v2.1.1](https://iiif.io/api/image/2.1/), with URLs in the form

```
{scheme}://{server}/{identifier}/{region}/{size}/{rotation}/{quality}.{format}
```

for instance

```
https://iiifserv.bdrc.io/igfn:bdr:I0886::08860035.tif/full/full/0/default.png
```

## Identifier form

The identifier can be of different forms:

##### igfn:{image_group}::{file_name} (canonical)

This is the canonical way to access an image, where:
- `{image_group}` is the prefixed version of the image group identifier in BDRC's data (example: `bdr:I0886`)
- `{file_name}` is the image file name (ex: `08860035.tif`) in our archive

BDRC makes a best effort commitment not to change the image file names, so that the url of an image can be considered persistent.

##### {image_group}::{file_name} (DEPRECATED)

This behaves like the `igfn:` prefixed version.

##### igsi:{image_group}::{simple_index}

In this version `image_group` has its usual meaning and `simple_index` is an integer representing the index of the image in the image list of the image group (ex: `35`). Note that BDRC does not make a commitment to the stability of these URLs. When accessing such a URL, the response includes a `Location:` statement indicating the URL in the canonical (`igfn:`) version.

## Preferred formats

It handles [compliance level 2](), with an extra property `preferredFormats`, taken from [IIIF Image API 3.0.0](https://iiif.io/api/image/3.0/#55-preferred-formats), which is important in our case because we have a lot of bitonal images that should be served as `png` and not `jpg` (to save processing time and bandwidth). Note that OpenSeaDragon handles this property since [v2.4.1](https://github.com/openseadragon/openseadragon/releases/tag/v2.4.1). 