# Notice on Updates, Changes, And support

This is a guide going over how updates will be handled with Accessories and how such effects its API and when/where features will be added:

## Features

In general Features will be brought to the latest target or generally when version differences do not inhibit current changes i.e. if rendering code changes between versions like 1.21.4 and 1.21.5 make it difficult to backport, then primary version will be on 1.21.5 and old versions will have critical bug fixes.

That means asking for a backport will either be given an answer of no or no answer at all as I can not answer every request given for such nor do I have all the time in the world to handle a specific version when developing new features if differences are massive.

Furthermore I have no answer to what version will be primarily support as it will most likely be that a popular version that I deem easiest to support will be chosen for LTS and the other supported version will be latest possible one.

## API Breakages

Such may occur with new releases as I attempt to better the design of the libraries API, add new features, fix underlining issues, or generally cleanup code base but such will not be done without some notice of deprecation on the given API. I will attempt to indicate when API will be switched to other alternatives as indicated within Javadocs but if no replacement API is possible, it will be indicated with such. If for some reason it is someone still requires such removed API, a discussion can occur about undoing such or bring an alternative if needed.

## Ports to other Loaders or other Versions

Overall if the desire for the given API to exist on older versions, you can fork such due to the License of the code being MIT similar to other Mod Loaders. It is best to ask permission to do such and with the understanding that it is marked unofficial and proper credit is given. Some mod loaders that are discouraged from being ported to for **NEWER** versions are Forge (aka Lex Forge) due to underling issues for leadership while older versions are possible.

In general all support will be handled by who ever created the port and any attempt to get aid from unsupported versions/loaders not clearly defined by the official project may not be answered or told to go else where. 