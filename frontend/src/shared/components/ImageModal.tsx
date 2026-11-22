interface ImageModalProps {
  isOpen: boolean;
  imageSrc: string;
  alt?: string;
  onClose: () => void;
}

function ImageModal({ isOpen, imageSrc, alt = 'Image', onClose }: ImageModalProps) {
  if (!isOpen) return null;

  return (
    <div
      className="fixed z-[1000] pt-0 left-0 top-0 w-full h-full overflow-hidden bg-[rgba(0,0,0,0.95)] flex justify-center items-center"
      onClick={onClose}
    >
      <span
        className="absolute top-4 right-9 text-gray-100 text-5xl font-bold cursor-pointer"
        onClick={onClose}
      >
        &times;
      </span>
      <img
        src={imageSrc}
        alt={alt}
        className="mx-auto block max-w-[90%] max-h-[90%] rounded"
        onClick={(e) => e.stopPropagation()}
      />
    </div>
  );
}

export default ImageModal;