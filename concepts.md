### Resources and resource metadata

Any build input or output. 

Normally files on filesystem but other types of inputs are supported. 

Resource status compared to the previous build NEW, MODIFIED, REMOVED or UNMODIFIED.

Resource metadata is read-only description of a resource that is part of the current build or was part of the previous build.

"Input" resources that is read during the build but not written to. Inputs are not expected to change during the build.

"Output" is a resource produced (or written to) during the build.

### Resource associations

Associations are generally directed. If resourceA is associated with resourceB this does not necessary mean that resourceB is associated with resourceA. This is optimization as in most cases associations only need to be queried one way.

Input->Output association is the most common and supported by dedicated API methods.

### ideas and questions

Resource labels. Input/output is related to what happens to the resource during the build. Labels show how resource is used. During java compilation, for example, 'source' means any .java resources fed to the compiler, it does not matter if the resource is input or output (which is the case for generated sources).
