### Resources and resource metadata

Any build input or output. 

Normally files on filesystem but other types of inputs are supported. 

Resource status compared to the previous build NEW, MODIFIED, REMOVED or UNMODIFIED.

Resource metadata is read-only description of a resource that is part of the current build or was part of the previous build.

"Input" resources that is read during the build but not written to. Inputs are not expected to change during the build.

"Output" is a resource produced (or written to) during the build.

### ideas and questions

Resource labels. Input/output is related to what happens to the resource during the build. Labels show how resource is used. During java compilation, for example, 'source' means any .java resources fed to the compiler, it does not matter if the resource is input or output (which is the case for generated sources).
