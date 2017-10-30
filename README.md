# eyetracking-data

Data collected for experiments with eyetracking during phonetic segmentation tasks.

## Video data

We have released a [5 min. preview] of the data.

For each subject, we provide a video in [Matroska] format (which can be played in [VLC] and other free software), including
- screen capture of the session (video stream #1)
- audio played back during the segmentation task
- rendered acoustic waveform and spectrogram for each scene, and gaze fixations (video stream #2)
- subtitles indicating start and end time of each scene in the segmented recording

The second video stream has been *reconstructed* from the segmented [audio], structured scene information, and gaze data (to be released).

## Background

For details, see the [poster] presented at ECEM 2015:
```bibtex
@InProceedings{Khan2015ECEM,
  author    = {Khan, Arif and Steiner, Ingmar and Macdonald, Ross and Sugano, Yusuke and Bulling, Andreas},
  title     = {Scene viewing and gaze analysis during phonetic segmentation tasks},
  booktitle = {18th European Conference on Eye Movements (ECEM)},
  year      = {2015},
  address   = {Vienna, Austria},
  month     = aug,
}
```
![ECEM 2015 poster](http://www.coli.uni-saarland.de/~steiner/publications/ECEMPoster.png)

[5 min. preview]: https://github.com/m2ci-msp/eyetracking-data/releases/tag/v1.0-preview
[Matroska]: https://www.matroska.org/
[VLC]: https://www.videolan.org/vlc/
[audio]: src/experiment/northwind_rm.flac
[poster]: http://www.coli.uni-saarland.de/~steiner/publications/ECEMPoster.pdf
