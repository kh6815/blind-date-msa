interface LoadingProps {
  fullScreen?: boolean;
  size?: 'sm' | 'md' | 'lg';
}

function Loading({ fullScreen = false, size = 'md' }: LoadingProps) {
  const sizeClasses = {
    sm: 'text-xl',
    md: 'text-2xl',
    lg: 'text-4xl',
  };

  if (fullScreen) {
    return (
      <div className="flex justify-center items-center h-screen absolute inset-0 bg-bg-color z-50">
        <i className={`fas fa-spinner fa-spin ${sizeClasses[size]} text-accent-color`}></i>
      </div>
    );
  }

  return (
    <div className="flex justify-center items-center p-5">
      <i className={`fas fa-spinner fa-spin ${sizeClasses[size]} text-accent-color`}></i>
    </div>
  );
}

export default Loading;